<?php

namespace App\Http\Controllers\CitRep;

use App\CitRep\Worker;
use App\Http\Controllers\Controller;
use Illuminate\Http\Request;

class WorkerController extends Controller
{

	/**
	* Action method, called from client worker
	*/
	public function action($action='') {
		
		//Reserve room for the response (JSONObject)
		$response = [];
		
		//Validate the password
		$status = $this->checkLogin(
			request()->input('p','')
		)?"OK":"Invalid password";
		
		//Obtain the worker name (globally unique)
		$workerID = request()->input('w','');
		
		//Obtain input string from worker
		$input = request()->input('i','');

		//Check if everything is ready to go
		if($status=="OK") {
			
			//Find worker object using uuid
			$worker = Worker::firstOrNew(['uuid' => $workerID]);
			
			//Actions that need to be handled in the controller
			switch($action) {
				case 'login':
				
					//Check version number of the input
					if(version_compare($input, Worker::$minVersion)===-1) {
						$status = "Worker version ".$input." is outdated, expecting minimum version ".Worker::$minVersion.". please update!";
					} else {
						$worker->heartbeat  = time();
						$worker->version    = $input;
						$worker->uuid		= $workerID;
						$worker->remoteaddr = isset($_SERVER['REMOTE_ADDR'])?$_SERVER['REMOTE_ADDR']:"";
						$worker->taskcount  = 0;
						$worker->save();
					}
					
				break;
				default:
					
					//Check if worker is loggedin
					if(strval($worker->version) == "") {
						$status = "Please login first!";
						
					} else {
						//Let worker perform the task, send back response
						$worker->heartbeat  = time();
						$response = $worker->process($action,$input);	
						$worker->save();
					}

				break;
			}
		}
		
		//Send resonse to client
		$response['s'] =  isset($response['s'])?$response['s']:$status;
		return response()->json($response);
	}
	
    /**
	* Check login for Worker
	* @return (bool) true if password is correct
	*/
    public function checkLogin($pass)
    {
        return ($pass==Worker::passphrase());
    }
}