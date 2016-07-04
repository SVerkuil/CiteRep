<?php

namespace App\CitRep;

use Illuminate\Database\Eloquent\Model;
use App\CitRep\Paper;
use App\CitRep\Task;

class Source extends Model {

    //These attributes are fillable
	protected $fillable = ['url','title','xpath_timestamp','xpath_title','xpath_doi','xpath_authors','xpath_abstract','xpath_type','xpath_faculty','xpath_group','xpath_pdf','xpath_year'];
	
	//These attributes contain xpaths that map to Paper objects
	protected $xpaths = [
		'xpath_timestamp' => 'timestamp',
		'xpath_title'=>'title',
		'xpath_doi'=>'doi',
		'xpath_authors'=>'authors',
		'xpath_abstract'=>'abstract',
		'xpath_type'=>'type',
		'xpath_faculty'=>'faculty',
		'xpath_group'=>'study',
		'xpath_pdf'=>'pdf_url',
		'xpath_year'=>'year'
		];
	
	//This is the resume token for next call to the parse() method
	protected $resumptionToken = '';
	
	//This is the amount of records parsed on last parse() call
	protected $recordsProcessed = 0;
	
	/**
	* Obtain Human Readable title
	*/
	public function title() {
		return ($this->title)?$this->title:'Unknown Source';
	}
	
	/**
	* Obtain color (gui)
	*/
	public function getColor() {
		$hash = md5('color' . $this->id);
		return '#'.substr($hash, 0, 6);
	}	
	
	/**
	* Obtain Human Readable description
	*/
	public function description() {
		$url   = $this->url?parse_url($this->url):[];
		$descr = '';
		if(isset($url['host'])) { 
			$descr .= 'Remote Host: '.$url['host'].'<br />'; 
		} else {
			$descr .= 'Remote Host: Undefined<br />'; 
		}
		$descr .= 'Last query: '.($this->last_query?$this->last_query:'Never').'<br />'; 
		$descr .= 'XML base: '.$this->xpath_base().'<br />';
		return $descr;
	}	
	
	/**
	* Remove this source from database
	* Process records, etc
	*/
	public function remove() {
		$this->delete();
		return true;
	}
	
	/**
	* Obtain partial XML data from url
	* Returns first 16kb
	*/
	public function getPartial() {
		$tmp = @file_get_contents($this->url,false,NULL,-1,1024*16);
		$tmp = str_replace("><",">\n<",$tmp);
		return strval($tmp);
	}
	
	/**
	* Helper to build URL
	* Returns url for XML request
	* Takes into account &from and &until parameters
	*
	* @param $resume (str) (optional) The resumptionToken to take into account
	*/
	protected function build_url($resume='') {
		$url   = $this->url;
		$parts = @parse_url($this->url);
		$add   = empty($parts['query'])?'?':'&';
		if($resume!='') {
			$url .= $add.'resumptionToken='.$resume;
			$add  = '&';
		}
		if($this->last_query!='') {
			$url .= $add.'from='.$this->last_query;
			$add  = '&';			
		}
		
		return $url;
	}
	
	/**
	* Parse source
	* Continues from last parsed timestamp
	* @param $resume (str) resumptionToken to send for this request
	* @return (Mixed) True if success, othwerwise String with error
	*/
	public function parse($resume='') {
		
		//Reset record processing counter
		$this->recordsProcessed = 0;
		
		//Attempt to stretch the server limits
		@ini_set('memory_limit', '512M');
		@set_time_limit( 600 );
		
		//Download XML file
		$xml_str = @file_get_contents($this->build_url($resume));
		if(!$xml_str) return 'Could not fetch remote file';
		
		//Parse XML file into SimpleXMLElement
		$xml = new \SimpleXMLElement($xml_str);
		if(!$xml) return 'Could not process remote file as valid XML';
		
		$used_ns = ''; //Register namespaces & store default namespace for xpath-query
		foreach($xml->getNamespaces(true) as $id => $ns) {
			if($id=='') { $id='query'; $used_ns = $id; }
			$xml->registerXPathNamespace($id,$ns);
		}
			
		//Query entries from base xpath
		$entries = $xml->xpath($this->xpath_base($used_ns));
		
		//Find resumptionToken (if set)
		$findToken = @$xml->xpath($this->xpath_base($used_ns,'ListRecords/resumptionToken'));
		$this->resumptionToken = (isset($findToken[0])?strval($findToken[0]):"");

		//Check if entries were found that we can process
		if(count($entries)==0) {
			return 'There were no (new) entries to process'; 
		}
		
		//Convert all xpaths to relative xpaths
		$relative_xpaths = [];
		foreach($this->xpaths as $xpath => $mapping) {
			$relative_xpaths[$xpath] = $this->xpath_base($used_ns,$this->getAttribute($xpath));
		}
		
		//Calculate extra xpath to see if item is deleted (OAI protocol only)
		$xpath_status = $this->xpath_base($used_ns,'/header/@status');

		//Loop over entries
		$the_base = $this->xpath_base(false);
		foreach($entries as $i => $entry) {
			
			$used_ns = ''; //Register namespaces & store default namespace for xpath-query
			foreach($entry->getNamespaces(true) as $id => $ns) {
				if($id=='') { $id='query'; $used_ns = $id; }
				$entry->registerXPathNamespace($id,$ns);
			}
			
			//Should we delete this entry? checks if header[status="deleted"] exists
			$checkDelete = @$entry->xpath($xpath_status);
			$delete = isset($checkDelete[0])?
						(strval($checkDelete[0]['status'])=='deleted'):false;
			
			//These values are to be used by the Paper model
			$toInsert = [];
			
			//Process all xpaths
			foreach($relative_xpaths as $xpath => $query) {
				
				//Produce values for the entry
				$search = @$entry->xpath($query);
				if(count($search)>1) {
					$result = [];
					foreach($search as $itm) {
						$result[] = strval($itm);
					}
				} else {
					$result = isset($search[0])?strval($search[0]):"";
				}
				
				$toInsert[$this->xpaths[$xpath]] = $result;
			
			}
			
			//Require the title and DOI to be known, otherwise we do not insert/update this item
			if(!empty($toInsert['doi']) && !empty($toInsert['title'])) { 
				
				//Up the counter
				$this->recordsProcessed = $this->recordsProcessed + 1;
				
				if(!$delete) { //Insert new paper
					$paper = Paper::firstOrNew(['doi' => $toInsert['doi']]);
					foreach($toInsert as $key => $val) {
						$paper->setAttribute($key,$val);
					} $paper->source_id = $this->id;
					$paper->save();
					
					//Create extract task for this paper
					Task::createNew('extract',$paper);
				}
			}
			
			//Check if we need to delete a record
			if(!empty($toInsert['doi']) && $delete) {
				
				//Up the counter
				$this->recordsProcessed = $this->recordsProcessed + 1;
				
				//Delete from database
				Paper::where('doi', $toInsert['doi'])->delete();
			}
			
		}
		
		//If there is no resumption token, and some records were processed, update timestamp
		if($this->resumptionToken=='' && $this->recordsProcessed > 0) {
			$this->last_query = date('Y-m-d\TH:i:s\Z');
			$this->save();
		}
		
		//If there is a resumption token (request succeeded) but nothing was processed, show failure
		if($this->resumptionToken!='' && $this->recordsProcessed == 0) {
			return 'Unable to process file, please check xpaths';
		}
		
		return true;
		
	}
	
	/**
	* Used to return resumptionToken after parse()
	* Empty if no token is present (hence no subsequent calls to parse should be made)
	*/
	public function getResumptionToken() {
		return $this->resumptionToken;
	}
	
	/**
	* Obtain the count of number of records processed at last call to parse()
	*/
	public function getRecordsProcessed() {
			return $this->recordsProcessed;
	}
	
	/**
	* Find document base from xpath array
	* Finds the smallest common part of the path
	* @param $ns (str)	The default namespace to use if no namespace is provided in the xpath
	* @param $subPath (str) The subPath to form relative to base path
	*/
	public function xpath_base($ns="",$subPath="") {
		$parts = null;
		foreach($this->xpaths as $xpath => $mapping) {
			$path  = trim($this->getAttribute($xpath));
				if(empty($path)) continue;
				if(substr($path,0,1)=='/') { $path = substr($path,1); }
			$split = explode('/',$path);
			if($parts===null) { 
				$parts = $split;
			} else {
				$new = [];
				foreach($parts as $i => $p) {
					if(isset($split[$i])) {
						if($split[$i]==$parts[$i]
						&& !empty($split[$i])) {
							$new[$i] = $split[$i];
						}
					}
				} $parts = $new;
			}
		} 
		
		//Should we make xpath relative (start with ./)
		$makeRelative = false;
		
		//If we need to process subPath, add it to the path now
		if($subPath!="") {
			$basePath = trim(implode('/',$parts));
			$subParts = explode('/',str_replace($basePath,"",$subPath));
			$parts    = [];
			foreach($subParts as $p) {
				if(!empty(trim($p))) { $parts[] = $p; }
			} $makeRelative = true;
		}
		
		//Add default namespace if needed
		if($ns!="") {
			$withNS = [];
			foreach($parts as $i => $part) {
				$query   = ($ns && (strpos($part,':')===false) && (strpos($part,'@')===false))?$ns.":":"";
				$withNS[$i] = $query.$part;
			} $parts = $withNS;
		}
		
		//If xpath is long enough, strip first token
		if(count($parts)>1 && $subPath=="") {
			unset($parts[0]);
		}
		
		//Return path, taking relative parameter into account
		if(empty($parts)) { return '/'; }
		return ($makeRelative?"./":"").trim(implode('/',$parts));
	}
	
	
}
