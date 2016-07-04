<?php

namespace App\Http\Controllers\CitRep;

use App\CitRep\Source;
use App\Http\Controllers\Controller;
use Illuminate\Http\Request;
use Validator, View;

class SourceController extends Controller
{

    /**
     * Get a validator for an incoming registration request.
     *
     * @param  array  $data
     * @return \Illuminate\Contracts\Validation\Validator
     */
    protected function validator(array $data)
    {
        return Validator::make($data, [
            'url' => 'required|active_url',
			'xpath_timestamp' => 'required',
			'xpath_title' => 'required',
			'xpath_doi' => 'required',
			'xpath_authors' => '',
			'xpath_abstract' => '',
			'xpath_year' => '',
			'xpath_type' => '',
			'xpath_faculty' => '',
			'xpath_group' => '',
			'xpath_pdf' => 'required'
        ]);
    }
	
	public function viewSources() {
		return View::make('pages.sources',['title'=>'Sources']);
	}
	
	public function createSource() {
		$source = Source::create();
		$id		= $source->id;
		return redirect('/source/edit/'.$id)
			->with('status', 'Source created');
	}
	
	public function editSource(Source $source) {
		return View::make('pages.source_edit',['source' => $source ]);
	}
	
	public function parseSource(Source $source, Request $request) {
		
		//Parse and check if all went well
		$msg = $source->parse($request->input('resume',''));
		if($msg===true) { $msg = 'OK'; }
		
		//Send response and additional info
		return response()->json([
			'msg'		=> $msg,
			'resume'	=> $source->getResumptionToken(),
			'count'		=> $source->getRecordsProcessed()
		]);
	}	
	
	public function deleteSource(Source $source) {
		$source->remove();
		return redirect('/source');
	}
	
	/**
	 * Handle a update of source field
	 */
	public function postSource(Source $source, Request $request)
	{
		//Validate URL input, code exits here if invalid	
		$this->validate($request, [
			'url' 	=> 'required|active_url',
			'title'	=> 'required'
		]);
		
		//Store all request variables
		$source->url 			= $request->input('url');
		$source->title 			= $request->input('title');
		$source->xpath_title 	= $request->input('xpath_title','');
		$source->xpath_doi 		= $request->input('xpath_doi','');
		$source->xpath_authors 	= $request->input('xpath_authors','');
		$source->xpath_abstract = $request->input('xpath_abstract','');
		$source->xpath_year		= $request->input('xpath_year','');
		$source->xpath_type 	= $request->input('xpath_type','');
		$source->xpath_faculty 	= $request->input('xpath_faculty','');
		$source->xpath_group 	= $request->input('xpath_group','');
		$source->xpath_pdf 		= $request->input('xpath_pdf','');
		$source->xpath_timestamp = $request->input('xpath_timestamp','');
		
		$source->save();

		//Redirect to full edit page or back to overview
		return $request->input('xpath_title','')?
				redirect('/source')
				:redirect('/source/edit/'.$source->id);
	}	

    /**
     * Create a new user instance after a valid registration.
     *
     * @param  array  $data
     * @return User
     */
    protected function create(array $data)
    {
        return User::create([
            'name' => $data['name'],
            'email' => $data['email'],
            'password' => bcrypt($data['password']),
        ]);
    }
}
