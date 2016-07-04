<?php

namespace App\CitRep;

use Illuminate\Database\Eloquent\Model;
use App\CitRep\Paper;
use DB;

class Task extends Model {

    //These attributes are fillable
	protected $fillable = ['uuid','status','task','parameters','completedby','cputime'];
	
	/**
	* Recycles old tasks that were stalled
	*/
	public static function updateTasks($max = 100) {
		
		/*
		* Recycle tasks that were never completed
		* Tasks have 3 hours time to finish, before recycling happens
		* Call this method on average once every 20 calls
		*/
		if(rand(0,20)==3) {
			DB::table('tasks')
				->where('status', 1) //status is assigned
				->where('completedby', "")
				->where('updated_at', '<', date('Y-m-d H:i:s',time()-(60*60*3)))
				->update(['status' => 0]); //set status to unassigned
		}

	}
	

	
	/**
	* Check if a task exists for a certain action on a paper object
	*/
	public static function exists($action,$paper) {
		$arr = Task::where('uuid',$action.'-'.$paper->id)->take(1);
		return count($arr)==1;
	}
	
	/**
	* Obtain parameters for this task
	*/
	public function getParams() {
		$params = $this->parameters;
		
		if(isset($params[0]) && isset($params[1]) && $this->task=="citation") {
			$paper   = Paper::find($params[0]);
			if($paper) {
				$params[1] = $paper->pdf_plain;
			}
		}
		
		return $params;
	}
	
	/**
	* Obtain a single unclompleted task that can be completed remotely
	* @param $lifo (deprecated) (bool) Set to true to use last-in-first-out
	*/
	public static function getUncompletedTask($lifo=false) {
		return Task::take(1)
					->where('status','0')
					->orderBy('id','asc')
					->get();		
	}
	
	/**
	* Complete this task based on $input
	* @param $worker (Worker) The worker which completed this task
	* @param $result (Mixed) The worker result for this task
	* @return (mixed) True if success, false or string on failure
	*/
	public function complete($worker, $result) {
		
		switch($this->task) {
			case 'extract':
				$paper = Paper::find($result['paperID']);
				if($paper) { //Paper could be deleted in the mean time
					$paper->pdf_plain = $result['paperTxt'];
					$paper->status = max(1,$paper->status);
					$paper->save();
				}
			break;
			case 'citation':
				$paper = Paper::find($result['paperID']);
				if($paper) { //Paper could be deleted in the mean time
					$paper->setCitationsAttribute($result['paperCit']);
					$paper->setJournalArray($result['paperJour']);
					$paper->status = max(2,$paper->status);
					$paper->save();
				}
			break;
			
		}
		
		//Complete this task
		$this->status = 2;
		$this->completedby = $worker->uuid;
		$this->cputime = intval(@$result['cputime']);
		$this->save();
		
		return true;
	}
	
	
	/**
	* Helper to create a task for a given action on given object
	* If the task already exists, it returns the task object from database
	* @return (obj) The task object
	*/
	public static function createNew($action,$object) {
		
		//Attempt to find task in database, or create new one
		$task = Task::firstOrNew(['uuid' => $action."-".$object->id]);
		
		if(!$task->id) {
			
			//If not found in database, fill it
			$task->uuid   = $action."-".$object->id;
			$task->status = 0;		
			$task->task   = $action;
			
			//Create task based on action
			switch($action) {
				
				case 'extract':
					$task->parameters = [$object->id,$object->pdf_url];
				break;
				case 'citation':
					$task->parameters = [$object->id,""]; //Fill content in getParam() to save DB storage
				break;
				
			}
			
			$task->save();
		}
	}
	
	//Serialize parameters into array
	public function setParametersAttribute($val) {
		$val = is_array($val)?$val:[$val];
        $this->attributes['parameters'] = serialize($val);
    }	
	
	//Unserialize parameters array
	public function getParametersAttribute($val) {
		return unserialize($val);
	}	
	
}