<?php

namespace App\CitRep;

use Illuminate\Database\Eloquent\Model;
use App\CitRep\Task;

class Worker extends Model {
	
	//Which version do we require as minimum for local Java workers?
	public static $minVersion = "1.0";
	
	//Where can the worker JAR be downloaded?
	private static $workerDownloadURI = "https://github.com/SVerkuil/CiteRep/tree/master/client";
	
    //These attributes are fillable
	protected $fillable = ['heartbeat','remoteaddr','taskcount','version'];	
	public $timestamps  = false;
	
	//Rate of heartbeat in seconds
	private static $heartbeatDelay = 5;
	
	//Grace time of heartbeat message in seconds
	private static $heartbeatGrace = 10;
	
	/**
	* Obtain URI where JAR can be downloaded
	*/
	public static function downloadURI() {
		return self::$workerDownloadURI;
	}
	
	/**
	* Obtain all currently connected workers
	* These workers can be assigned tasks which they will perform locally
	*/
	public static function getConnected() {
		return Worker::orderBy('uuid', 'asc')
		 ->where('heartbeat', '>=', time() - self::$heartbeatDelay - self::$heartbeatGrace )
		 ->get();
	}
	
	/**
	* Obtain worker remote machine name
	* @return (str) String representation of machine name
	*/
	public function machineName() {
		$uuid = explode('@',$this->uuid,4);
		return isset($uuid[0])?$uuid[0]:"";
	}
	
	/**
	* Obtain worker time started
	* @return (int) timestamp in milliseconds
	*/
	public function startTime() {
		$uuid = explode('@',$this->uuid,4);
		return isset($uuid[1])?intval($uuid[1]/1000):time();
	}
	
	/**
	* Obtain the user given identifier of the remote machine
	* @return (str) User identifier string
	*/
	public function givenName() {
		$uuid = explode('@',$this->uuid,4);
		return isset($uuid[3])?$uuid[3]:md5($this->uuid);
	}	
	
	/**
	* Is this worker online at this moment?
	*/
	public function isOnline() {
		return $this->heartbeat > (time() - $this->heartbeatDelay - $this->heartbeatGrace);
	}
	
	
	/**
	* How long was this worker connected in seconds?
	* @return (str) Human readable time elapsed since connecting
	*/
	public function connectionTime() {
		$seconds = max(0,$this->heartbeat - $this->startTime());
		$dtF	 = new \DateTime("@0");
		$dtT	 = new \DateTime("@".$seconds);
		$format  = ($seconds>(60*60))?"%a days, %h hours, %i minutes and %s seconds":"%i minutes and %s seconds";
		return $dtF->diff($dtT)->format($format);		
	}
	
	/**
	* Produce HTML dump of paper contents
	* @param $return (bool) Should we return HTML or echo it?
	*/
	public function dump($return=true) {
		$html = '<div class="box box-solid">
			<div class="box-header with-border">
				  <i class="fa fa-desktop"></i>
				  <h3 class="box-title">'.$this->givenName().' (v'.$this->version.') </h3>
				</div>
			<div class="box-body">
			<table class="table table-striped">
				  <tbody>';
			$html .= '<tr>
						<th style="max-width:100px;">Machine Name</th>
						<td>'.$this->machineName().'</td>
					  </tr>';
			$html .= '<tr>
						<th style="max-width:100px;">Duration</th>
						<td>'.$this->connectionTime().'</td>
					  </tr>';
			$html .= '<tr>
						<th style="max-width:100px;">Tasks Performed</th>
						<td>'.$this->taskcount.'</td>
					  </tr>';
			$html .= '<tr>
						<th style="max-width:100px;">Remote Address</th>
						<td>'.$this->remoteaddr.'</td>
					  </tr>';
		$html .= '</tbody>
				</table></div></div>';
		
		//Produce output
		if($return) { return $html; }
		echo $html;
	}	

	/**
	* Obtain passphrase for connecting
	* workers to this system. This is
	* a basic protection layer
	*/
	public static function passphrase() {
		
		//Generate short token based on environment key
		$app_key = env('APP_KEY');
		return strtoupper(hash("crc32b", $app_key));
	}
	
	/**
	* Let this worker instance process a task
	* @param $action (str)	The action to perform
	* @param $input  (mixed) The action input
	* @return (array), optionally ['s'] contains error message
	*/
	public function process($action,$input) {
		$r = []; //Response based on this action query
		
		switch($action) {
			
			case 'heartbeat':
			
				//Recycle possibly stalled tasks
				Task::updateTasks();
				
				//New tasks have incrementing ID, we select oldest first
				//Status must either be 0 (uncompleted)
				$find = Task::getUncompletedTask();
					
				if(isset($find[0])) {
					$task = $find[0];
					$task->status = 1;
					$task->save();
					
					$r['uuid']  = $task->uuid;
					$r['task']  = $task->task;
					$r['param'] = $task->getParams();
					
				} else {
					$r['uuid'] = '';
					$r['task'] = '';
				}

			break;
			
			case 'completed':
				$json = @json_decode($input,true);
				if(!$json) { 
					$r['s'] = 'Invalid JSON response'; 
				
				} else if(isset($json['uuid'])) {
					$uuid = $json['uuid'];
					$task = Task::firstOrNew(['uuid' => $uuid]);
					
					if(!$task->id) {
						$r['s'] = 'Task UUID not found in database'; 
					
					} else {

						//Complete task, update taskcount and save
						$status = $task->complete($this,$json);
						if($status===true) {
							$this->taskcount = $this->taskcount + 1;
							$this->save();
						} else {
							$r['s'] = $status?$status:'Task could not be completed'; 
						}
					
					}
				
				} else {
					$r['s'] = 'Please specify to which task UUID this response belongs'; 
				}
			break;
			
			default;
				$r['s'] = 'Unknown Worker action';
			break;
		}
		
		return $r;
		
	}
	
}