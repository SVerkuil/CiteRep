<?php

namespace App\CitRep;

use Illuminate\Database\Eloquent\Model;
use App\CitRep\Source;
use App\CitRep\Task;
use DB;

class Paper extends Model
{
	
	//These attributes are fillable
	protected $fillable = ['title','doi','authors','abstract','year','type','faculty','study','pdf_plain','pdf_url','citations','timestamp','source_id',
	'status']; //Status 0 = created, 1 = text parsed, 2 = citations extracted, 3 = journals found
	
	/**
	* Produce HTML dump of paper contents
	* @param $return (bool) Should we return HTML or echo it?
	*/
	public function dump($return=true) {
		$id = ($this->id)?$this->id:"Not in Database";
		
		
		$html = '<div class="nav-tabs-custom">
			<script type="text/javascript">function loadpdf'.$this->id.'() {
				var s = $("div[data-pdf=\''.$this->id.'\']");
				if(s.html()=="") { s.html("<iframe src=\'/publication/view/'.$this->id.'/0\' style=\'width:100%;height:600px;\'></iframe>") }
			}</script>
            <ul class="nav nav-tabs">
              <li class="active"><a aria-expanded="true" href="#tab_1_'.$this->id.'" data-toggle="tab">Info</a></li>';
			  if(!empty($this->pdf_url)) { $html .= '<li class=""><a aria-expanded="false" href="#tab_2_'.$this->id.'" data-toggle="tab" onclick="loadpdf'.$this->id.'()">PDF</a></li>'; }
              $html .= '<li class=""><a aria-expanded="false" href="#tab_3_'.$this->id.'" data-toggle="tab">Text</a></li>
              <li class=""><a aria-expanded="false" href="#tab_4_'.$this->id.'" data-toggle="tab">Citations</a></li>
			  <li class=""><a aria-expanded="false" href="#tab_5_'.$this->id.'" data-toggle="tab">Journals</a></li>
            </ul>
            <div class="tab-content">
              <div class="tab-pane active" id="tab_1_'.$this->id.'">
			<table class="table table-striped">
				  <tbody>
				  <tr>
					<th style="max-width:100px;">Doi</th>
						<td>'.$this->doi.'</td>
				  </tr>				  
				  <tr>
					<th style="max-width:100px;">Title</th>
						<td>'.$this->title.'</td>
				  </tr>
				  <tr>
					<th style="max-width:100px;">Year</th>
						<td>'.$this->year.'</td>
				  </tr>
				  <tr>
					<th style="max-width:100px;">Faculty / Programme</th>
						<td>'.$this->faculty.' / '.$this->study.'</td>
				  </tr>
				  <tr>
					<th style="max-width:100px;">Authors</th>
						<td><ul><li>'.implode('</li><li>',$this->authors).'</li></td>
				  </tr>
				  <tr>
					<th style="max-width:100px;">Source</th>
						<td>'.Source::find($this->source_id)->title.'</td>
				  </tr>
				  <tr>
					<th style="max-width:100px;">PDF url</th>
						<td>'; foreach($this->pdf_url as $url) {
							$html .= '<a href="'.$url.'" target="_blank">'.$url.'</a><br />';
						} $html .='</td>
				  </tr>				  
				  <tr>
					<th style="max-width:100px;">Abstract</th>
						<td>'.$this->abstract.'</td>
				  </tr></tbody>
				</table>
              </div>
              <div class="tab-pane" id="tab_2_'.$this->id.'">
                <div data-pdf="'.$this->id.'"></div>
              </div>			  
              <div class="tab-pane" id="tab_3_'.$this->id.'">
                <pre style="max-height:600px;">'.$this->getFormattedText().'</pre>
              </div>
              <div class="tab-pane" id="tab_4_'.$this->id.'">
                <pre style="max-height:600px;">'.$this->getFormattedCitations().'</pre>
              </div>
              <div class="tab-pane" id="tab_5_'.$this->id.'">
                <pre style="max-height:600px;">'.$this->getFormattedJournals().'</pre>
              </div>			  
            </div>
          </div>';
		if($return) { return $html; }
		echo $html;
	}
	
	/**
	* If a new paper is saved, take action
	*/
	public function save(array $options = array()) {
		
		//Run the actual save() method on parent class
		$return = parent::save($options);
		
		//Extact task is created in Source.parse()
		
		//If the object exits and is extracted, create citation task (if not already created)
		if(intval($this->status)===1) {
			Task::createNew('citation',$this);
		}
		
		//Return output
		return $return;
	}
	
	/**
	* GUI formatting
	*/
	public function getFormattedText() {
		$plain = str_replace(chr(2),"\n",$this->pdf_plain);
		$plain = str_replace(chr(1),"",$plain);
		$plain = trim($plain);
		if(!$plain) { $plain = "-- This document has no text content --"; }
		if($this->pdf_plain=="" && $this->status==0) { $plain = "-- Document not yet processed --"; }
		return $plain;
	}
	
	public function getFormattedCitations() {
		$arr = $this->citations;
		if(empty($arr) && $this->status==2) { 
			$arr = ["-- No citations found --"]; 
		} else if(empty($arr)) { 
			$arr = ["-- Citations not yet extracted --"]; 
		}
		
		return implode("\n",$arr);
	}	
	
	public function getFormattedJournals() {
		$journals = DB::select('select * from journals where paper_id = ? ORDER BY journal ASC', [$this->id]);
		$html = '';
		foreach ($journals as $j) {
			$html .= $j->journal."\n";
		} return $html.'';
	}
	
	//Parse a array containing the journals for this paper
	//[{j:"journal",n:"normalized"},{...},...]
	public function setJournalArray($arr) {
		DB::delete('delete from journals WHERE paper_id=?',[$this->id]);
		if(!empty($arr) && is_array($arr)) {
			foreach($arr as $itm) {
				DB::insert('insert into journals (paper_id,journal,normalized) values (?, ?, ?)', [
					$this->id, $itm['j'], $itm['n']]);
			}
		}
	}	
	
	/**
	* Serializers and unserializers
	* Used for arrays
	*/	
	
    public function setStudyAttribute($val) {
		$val = is_array($val)?reset($val):$val;
		$val = str_replace(array('Programme:','programme:'),'',$val);
        $this->attributes['study'] = $val;
    }		
	
    public function setFacultyAttribute($val) {
		$val = is_array($val)?reset($val):$val;
		$val = str_replace(array('Faculty:','faculty:'),'',$val);
        $this->attributes['faculty'] = trim($val);
    }	
	
    public function setAuthorsAttribute($val) {
		$val = is_array($val)?$val:[$val];
        $this->attributes['authors'] = serialize($val);
    }	
	
    public function setCitationsAttribute($val) {
		$val = is_array($val)?$val:[$val];
        $this->attributes['citations'] = serialize($val);
    }		
	
    public function setPdfUrlAttribute($val) {
		$val = is_array($val)?$val:[$val];
        $this->attributes['pdf_url'] = serialize($val);
    }
	
	public function getPdfUrlAttribute($val) {
		return unserialize($val);
	}		
	
	public function getCitationsAttribute($val) {
		return unserialize($val);
	}	
	
	public function getAuthorsAttribute($val) {
		return unserialize($val);
	}
}
