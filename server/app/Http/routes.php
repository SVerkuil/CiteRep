<?php
/*
|--------------------------------------------------------------------------
| Application Models
|--------------------------------------------------------------------------
*/
Route::model('source', 'App\CitRep\Source');

/*
|--------------------------------------------------------------------------
| Application Routes
|--------------------------------------------------------------------------
|
| This route group applies the "web" middleware group to every route
| it contains. The "web" middleware group is defined in your HTTP
| kernel and includes session state, CSRF protection, and more.
|
*/

Route::group(['middleware' => ['web']], function () {
	
	//Login authentication
	Route::get('/login', 'Auth\AuthController@getLogin');
	Route::post('/login', 'Auth\AuthController@postLogin');
	Route::get('/logout', 'Auth\AuthController@logout');
    
	//Pages only visible after successfull login
	Route::group(['middleware' => ['auth']], function () {
		
		//Homepage
		Route::get('/', 'CitRep\DashboardController@viewDashboard');
		
		//Source handling
		Route::get('/source', 'CitRep\SourceController@viewSources');
		Route::get('/source/create', 'CitRep\SourceController@createSource');
		Route::get('/source/edit/{source}', 'CitRep\SourceController@editSource');
		Route::post('/source/edit/{source}', 'CitRep\SourceController@postSource');
		Route::get('/source/delete/{source}', 'CitRep\SourceController@deleteSource');
		Route::post('/source/parse/{source}', 'CitRep\SourceController@parseSource');
		
		//Publication handling
		Route::get('/publication', 'CitRep\PaperController@viewPapers');
		Route::post('/publication', 'CitRep\PaperController@postSearch');
		
		//Journal citation reports
		Route::get('/reports', 'CitRep\ReportController@viewReport');
		Route::post('/reports', 'CitRep\ReportController@postSearch');
		
		//Workers
		Route::get('/workers', function () {
			return view('pages.workers');
		});
		
		//View PDF fullscreen
		Route::get('/publication/view/{paper}/{num}', 'CitRep\PaperController@viewPdf');
		Route::get('/publication/pdf/{paper}/{num}.pdf', 'CitRep\PaperController@pipePdf');
	
	});
});


/*
|--------------------------------------------------------------------------
| Worker Routes
|--------------------------------------------------------------------------
|
| This route group applies the "worker" connected to this web app
*/

//Basic passphrase check done using post
Route::post('/workers/{action}', 'CitRep\WorkerController@action');