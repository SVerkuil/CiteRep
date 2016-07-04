<?php

namespace App\Http\Controllers\CitRep;

use App\CitRep\Paper;
use App\CitRep\Source;
use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use Validator, View, DB;

class ReportController extends Controller {
	
	/**
	 * User requested to generate a detailed report
	 * Perform database lookup and return results
	 */
	public function postSearch(Request $request) {
		
		//Start year and timespan
		$year_start	= intval($request->input('year',date('Y')));
		$timespan	= abs(intval($request->input('timespan',1)));
		$year_end	= $year_start + ($timespan-1);
		$range		= range($year_start,max($year_start,$year_end));
		
		//Filter criteria
		$sources		= $request->input('source',[]);
		$faculties		= $request->input('faculty',[]);
		$studies		= $request->input('study',[]);
		$journals		= $request->input('journal',[]);
		
		//List all faculties
		$all_faculties = DB::table('papers')->distinct()->orderBy('faculty','asc')->get(['faculty']);
		
		//Construct SQL query to generate report
		$tags = [];
		$sql = 'SELECT 
			GROUP_CONCAT(DISTINCT j.journal) AS journal,
			normalized,
			COUNT(*) AS '.md5('total');
			
		//Data headers (description for each column in SQL result)
		$headers = ['Journal notations','Normalized notation','Times Referenced'];
			
		//Add year trend columns
		foreach($range as $year) {
			$sql .= "\r\n".', SUM(IF(p.year=?,1,0)) AS "total'.$year.'"';
			$tags[] = $year; $headers[] = $year;
		}
		
		//Add faculty columns
		foreach($all_faculties as $fac) {
			$hash = md5($fac->faculty);
			$sql .= "\r\n".', SUM(IF(p.faculty=?,1,0)) AS "'.$hash.'"';
			$tags[] = $fac->faculty; $headers[] = 'Faculty '.$fac->faculty;
		}		
		
		//Add basic filter criteria
		$sql .=' FROM
					journals j,
					papers p
				WHERE
					j.paper_id=p.id
				AND
					p.year >= ?
				AND 
					p.year <= ?';
		$tags[] = $year_start;
		$tags[] = $year_end;
					
		//Add sources filter criteria
		if(!empty($sources)) {
			$sql .= ' AND  p.source_id IN('.implode(',',array_fill(0,count($sources),'?')).')';
			foreach($sources as $x) { $tags[] = $x; }
		} else {
			$sql .= ' AND false'; //No source specified, do nothing
		}
		
		
		//Add Faculties filter criteria
		if(!empty($faculties)) {
			$sql .= ' AND  p.faculty IN('.implode(',',array_fill(0,count($faculties),'?')).')';
			foreach($faculties as $x) { $tags[] = $x; }
		}		
		
		//Add Studies filter criteria
		if(!empty($studies)) {
			$sql .= ' AND p.study IN('.implode(',',array_fill(0,count($studies),'?')).')';
			foreach($studies as $x) { $tags[] = $x; }
		}	

		//Add Journals filter criteria
		if(!empty($journals)) {
			$sql .= ' AND j.normalized IN('.implode(',',array_fill(0,count($journals),'?')).')';
			foreach($journals as $x) { $tags[] = $x; }
		}		
					
		//Run SQL query
		$sql .= ' GROUP BY j.normalized';
		$data = DB::select(DB::raw($sql),$tags);
		$data = json_decode(json_encode($data), true);
		
		##
		# Calculate overall & faculty journal top-25
		#
		if(empty($faculties)) { foreach($all_faculties as $fac) { $faculties[] = $fac->faculty; } }
		$facList = []; foreach($faculties as $f) { $facList[] = $f?$f:'Other / Unknown'; } 
		
		//Add total count to list of 'faculties' (list to be processed into tabs on the interface)
		array_unshift($faculties,'total');
		
		//Prepare array which is used in reporthtml.blade
		$arr = ['top25'=>[],'years'=>$range,'faculties'=>$facList];
		
		//Loop over faculties
		foreach($faculties as $fac) {
			$tmp = [];
			$hash = md5($fac);
			
			//Sort the data array and generate top-25 for this faculty
			usort($data, function($a, $b) use ($hash) { return ($b[$hash] - $a[$hash]); });
			for($i=0;$i<min(25,count($data));$i++) {
				$count = $data[$i][$hash];
				if($count>0) {
					
					//Add journal and aggregated count
					$add = ['j'=>explode(',',$data[$i]['journal']),'n'=>$data[$i]['normalized'],'c'=>$count];
					
					//Add total global (not specific to this faculty) occurences of that journal for each year
					foreach($range as $year) {
						$add[$year] = $data[$i]['total'.$year];
					} $add['total'] = $data[$i][md5('total')];
					
					//Add distribution among faculties to this entry
					//This distribution is used to generate a pie chart
					$pie = [];
					foreach($faculties as $fc) {
						$h = md5($fc);
						if($fc!='total') { //Do not add the total as this will end up in piechart
							$pie[] = $data[$i][$h];
						}
					} $add['pie'] = implode(',',$pie);
					
					
					$tmp[] = $add;
				}
			}			
			
			//Check if we found data for this faculty, rename empty faculty to 'other/unknown'
			if(!empty($tmp)) {
				$arr['top25'][$fac?$fac:'Other / Unknown'] = $tmp;
			}
		}
		
		//Calculate bar / line chart items
		$hash = md5('total'); //Sort most refererenced journal (total count)
		usort($data, function($a, $b) use ($hash) { return ($b[$hash] - $a[$hash]); });
		
		//Fill data points
		$dataset = [];
		for($i=0;$i<min(25,count($data));$i++) {
			$itm = ['label'=>$data[$i]['normalized'],'data'=>[]];
			foreach($range as $year) {
				$itm['data'][] = $data[$i]['total'.$year];
			} $dataset[] = $itm;
		} $arr['chart'] = $dataset;
		
		//Return HTML or CSV download (based on request)
		$dl = $request->input('download',0);
		
		if($dl) {
			array_unshift($data,$headers); //Prepend headers to CSV export
			return $this->download($data);
		} else {
			return View::make('pages.reporthtml',$arr);
		}
	}	
	
	
	/**
	* Render output to be downloaded as CSV by the client
	*/
	protected function download($list) {
		$headers = [
				'Cache-Control'       => 'must-revalidate, post-check=0, pre-check=0'
			,   'Content-type'        => 'text/csv'
			,   'Content-Disposition' => 'attachment; filename='.date('Y-m-d-H-i-s').'-citerep.export.csv'
			,   'Expires'             => '0'
			,   'Pragma'              => 'public'
		];

	   $callback = function() use ($list)  {
			$FH = fopen('php://output', 'w');
			foreach ($list as $row) { 
				fputcsv($FH, $row,';');
			}
			fclose($FH);
		};

		return \Response::stream($callback, 200, $headers);
	}
	
	
	/**
	* Display journal citation reports view
	*/
	public function viewReport() {
		
		//List of years in database
		$years = DB::table('papers')->distinct()->orderBy('year','desc')->get(['year']);
		
		//List of faculties in database
		$faculties = DB::table('papers')->distinct()->orderBy('faculty','asc')->get(['faculty']);
		
		//List of studies in database
		$studies = DB::table('papers')->distinct()->orderBy('study','asc')->get(['study']);
		
		//List of journals in database (limit to 1000 most popular)
		$journals = DB::table('journals')
						->groupBy('normalized')
						->orderBy('cnt','desc')
						->limit(1000)
						->get(['journal','normalized',DB::raw('COUNT(*) AS cnt')]);
		usort($journals, function($a, $b) {
			return strcmp($a->journal, $b->journal);
		});				
		
		//Find valid sources
		$sources = Source::orderBy('title', 'asc')->get();
		if(empty($this->search_sources)) {
				foreach($sources as $s) { $this->search_sources[] = $s->id; }
		}		
		
		
		return View::make('pages.reports',[
							'title'=>'Journal Citation Reports',
							'years'=>$years,
							'sources'=>$sources,
							'faculties'=>$faculties,
							'studies'=>$studies,
							'journals'=>$journals]);				
	}
}