<?php

namespace App\Http\Controllers\CitRep;

use App\CitRep\Paper;
use App\CitRep\Source;
use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use Validator, View, DB;

class PaperController extends Controller {
	
	//Search filters
	protected $search_years = "2000,2016";	//Year_start,Year_end
	protected $search_sources = [];	//id,id,id
	protected $minStatus = 0;
	
	//Search for free text (title and abstract)
	protected $search_title = '';
	
	
	//Search range
	protected $year_min = 1900;
	protected $year_max = 2000;

	/**
	* PIPE PDF file through web server
	*/
	public function pipePdf(Paper $paper,$num) {
		//Render the view page
		$urls = $paper->pdf_url;
		$num  = intval($num);
		$url  = isset($urls[0])?$urls[0]:"";
		if(isset($urls[$num])) { $url = $urls[$num]; }
		$content = $url?@file_get_contents($url):false;
		if(empty($content)) { //PDF no longer exists or is inaccessible from this IP
			$content = file_get_contents(asset('plugins/pdfjs/web/unavailable.pdf'));
		}
		return response($content)
				->header('Content-Type', 'application/pdf');	
	}	
	
	
	/**
	* View a specific PDF file in PDFjs
	*/
	public function viewPdf(Paper $paper,$num,Request $r) {
		//Render the view page
		$urls = $paper->pdf_url;
		$num  = intval($num);
		$url  = isset($urls[0])?$urls[0]:"";
		if(isset($urls[$num])) { $url = $urls[$num]; }
		
		return View::make('pages.pdfview',[
				'pdf_url'=>str_replace('/view/','/pdf/',$r->url()).'.pdf',
				]);		
	}
	
	/**
	 * Handle a update of source field
	 */
	public function postSearch(Request $request) {
		$this->search_years   = $request->input('years',"");
		$this->search_sources = $request->input('sources',[]);
		$this->minStatus 	  = $request->input('minStatus',0);
		$this->search_title	  = $request->input('title',"");
		return $this->viewPapers();
	}
	
	/**
	* View papers
	*/
	public function viewPapers() {
		
		//Calculate the minimum year
		//Set the search years range
		$oldest = Paper::orderBy('year', 'asc')
				 ->where('year','>',1900)
				 ->take(1)
				 ->get();
		if(count($oldest)>0) {
			$this->year_min = $oldest[0]->year;
		} if(!$this->search_years) {
			$this->search_years = $this->year_min.",".date('Y');
		}
		
		//Find the minimum and maximum
		$y = explode(',',$this->search_years,2);
		$this->year_max = $y[1];
		
		//Find valid sources
		$sources = Source::orderBy('title', 'asc')->get();
		if(empty($this->search_sources)) {
				foreach($sources as $s) { $this->search_sources[] = $s->id; }
		}
		
		//Create search variable
		$papers = Paper::where('year','>=',$y[0])
				->where('year','<=',$y[1])
				->orderBy('year', 'asc')
				->orderBy('doi', 'asc')
				->where('status','>=',$this->minStatus);
				
		if($this->search_title!="") {
			$papers = $papers->where('title','like','%'.$this->search_title.'%');
		}
				
				
		$papers = $papers->whereIn('source_id',$this->search_sources)
				->paginate(10);
		
		//Render the view page
		return View::make('pages.papers',[
				'title'=>'Papers',
				'papers'=>$papers,
				'sources'=>$sources,
				'year_min'=>$this->year_min,
				'search_years'=>$this->search_years,
				'search_sources'=>$this->search_sources,
				'minStatus'=>intval($this->minStatus),
				'js_linechart'=>json_encode($this->getLineChartVariables()),
				'js_barchart'=>json_encode($this->getBarChartVariables()),
				'search_title'=>$this->search_title
				]);
	}	
	
	private function fixProgrammeName($name) {
		$parts = explode(' (',$name,2);
		$name = str_replace('department of','',strtolower($parts[0]));
		return substr($name,0,12);
	}
	
	/**
	* Get all variables needed to render the bar chart
	*/
	protected function getBarChartVariables() {
		$datasets = [];
		$range    = explode(',',$this->search_years,2);
		$tmp 	  = DB::select("SELECT papers.faculty AS data, COUNT(faculty) AS cnt FROM papers
					WHERE year>=? AND year<=? AND status>=? GROUP BY papers.faculty ORDER BY papers.faculty ASC", 
					[$range[0],$range[1],$this->minStatus]);
		$labels   = [];
		foreach($tmp as $t) { if($t->data) { $labels[] = $this->fixProgrammeName($t->data); } }
		$sources  = Source::orderBy('title', 'asc')->get();
		foreach($sources as $source) {
			if(!in_array($source->id,$this->search_sources)) { continue; }
			$color = $source->getColor();
			$tmp   = DB::select("SELECT papers.faculty AS data,COUNT(*) AS counter FROM papers WHERE year>=? AND year<=? AND source_id=? AND status>=? GROUP BY papers.faculty",
					[$range[0],$range[1],$source->id,$this->minStatus]);
			$data  = array_flip($labels);
			foreach($tmp as $obj) {
				if($obj->data) {
					$data[$this->fixProgrammeName($obj->data)] = min(100,$obj->counter);
				}
			}
			$dataset = [
			  "label" => $source->title,
			  "backgroundColor" => $color,
			  "fill" => false,
			  "data" => array_values($data)
			]; $datasets[] = $dataset;
		}
		
		
		return [
			  "labels" => $labels,
			  "datasets" => $datasets
			];
			
	}	
	
	/**
	* Get all variables needed to render the line chart
	*/
	protected function getLineChartVariables() {
		$datasets = [];
		$range    = explode(',',$this->search_years,2);
		$labels    = range($range[0],$range[1]);
		$sources  = Source::orderBy('title', 'asc')->get();
		foreach($sources as $source) {
			if(!in_array($source->id,$this->search_sources)) { continue; }
			$color = $source->getColor();
			$tmp   = DB::select("SELECT year as data,COUNT(year) AS counter FROM papers WHERE year>=? AND year<=? AND source_id=? AND status>=? GROUP BY year",
					[$range[0],$range[1],$source->id,$this->minStatus]);
			$data  = array_flip($labels);
			foreach($tmp as $obj) {
				$data[$obj->data] = $obj->counter;
			} if(empty($tmp)) { $data = []; }
			$dataset = [
			  "label" => $source->title,
			  "backgroundColor" => $color,
			  "fill" => false,
			  "data" => array_values($data)
			]; $datasets[] = $dataset;
		}
		
		
		return [
			  "labels" => $labels,
			  "datasets" => $datasets
			];
			
	}

}