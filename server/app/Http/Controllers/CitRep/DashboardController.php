<?php

namespace App\Http\Controllers\CitRep;

use App\CitRep\Paper;
use App\CitRep\Worker;
use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use Validator, View, DB;

class DashboardController extends Controller {
	
	/**
	* Obtain contents for the dashboard from the database
	*/
	public function viewDashboard() {
		
		//Calculate how much time was spend in total by workers processing tasks
		$cputime = $this->secondsToTime(DB::table('tasks')->sum('cputime'));
		
		//Check how many workers are currently connected
		$workers_current = count(Worker::getConnected());
		
		//Count how many tasks were completed by workers
		$tasks_completed = DB::table('tasks')->where('status','=','2')->count();
		
		//Count how many tasks are not yet completed by workers
		$tasks_uncompleted = DB::table('tasks')->where('status','!=','2')->count();		
		
		//Count how many entries we have in our papers database
		$papers = DB::table('papers')->count();
		
		//And how many papers yielded extracted journal referencess
		$journals = DB::table('journals')->distinct('paper_id')->count('paper_id');

		return View::make('pages.index',[
							'title'=>'Dashboard',
							'cputime'=>$cputime,
							'workers'=>$workers_current,
							'completed'=>$tasks_completed,
							'uncompleted'=>$tasks_uncompleted,
							'papers'=>$papers,
							'journals'=>$journals]);		
	}	
	
	//Helper to convert milliseconds to days, hours, minutes and seconds
	private function secondsToTime($ms) {
		$seconds = floor($ms/1000);
		$dtF = new \DateTime('@0');
		$dtT = new \DateTime("@$seconds");
		return $dtF->diff($dtT)->format('%a days, %h hours, %i minutes and %s seconds');
	}

}