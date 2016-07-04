@section('scripts')
<script type="text/javascript" src="{{ URL::asset('citerep/xpath.js') }}"></script>
@endsection
@extends('app')
@section('content')
@if (session('status'))
    <div class="alert alert-success">
        {{ session('status') }}
    </div>
@endif
@if ($errors->has())
<div class="alert alert-danger">
	@foreach ($errors->all() as $error)
		{{ $error }}<br>        
	@endforeach
</div>
@endif

		<div class="col-sm-12">
			<div class="box box-info">
				<div class="box-header with-border">
				  <h3 class="box-title">Edit {{ $source->title() }}</h3>
				  <div class="box-tools pull-right">
					<button type="button" class="btn btn-box-tool" data-widget="collapse"><i class="fa fa-minus"></i>
					</button>
				  </div>
				</div>
				<form method="POST" action="">
				{!! csrf_field() !!}
				@if ($source->url && $source->title)
					<div class="box-body">
					  <div class="row">
						<div class="col-xs-12">
							<p class="text-center">
							Use this tool to specify where the information resides in a XML document. Click on a input field and after that in the textarea to automatically generate a <a href="http://www.w3.org/TR/xpath/" target ="_blank">xpath</a>. This tool works best with resources conforming to the <a href="https://www.openarchives.org/OAI/openarchivesprotocol.html" target="_blank">Open Archives Initiative</a> standard. This means that the source url will be appended with <i>&amp;from</i> and <i>&amp;unitl</i> query parameters to obtain only the changes since the last query. Deleted records will only be processed correctly by this system if the external resource maintains <i>persistent</i> information about deleted records. If the remote XML source provides a <i>resumptionToken</i> it will be used to generate subsequent requests to obtain all information.
							</p>						
						</div>
					  </div>
					  <div class="row">
						<div class="col-md-7">
							<div class="description-block">
								<h5>XML preview</h5>
								 <div class="form-group">
									<textarea id="in" class="form-control" rows="20">{{$source->getPartial()}}</textarea>
								</div>
							</div>
						</div>
						<div class="col-md-5">
							<div class="description-block">
								<h5>Source info</h5>
								<div class="input-group">
									<span class="input-group-addon" style="min-width:100px;">Title</span>
									<input class="form-control" type="text" name="title" placeholder="Title" value="{{ old('title',$source->title) }}" required>
								</div>		
								<div class="input-group">
									<span class="input-group-addon" style="min-width:100px;">Source URL</span>
									<input name="url" type="text" class="form-control" placeholder="http://" value="{{ old('url',$source->url) }}" required>
								</div>									
								<h5>XPATH selectors</h5>
								<div id="xpath_div">
									<div class="input-group">
										<span class="input-group-addon" style="min-width:200px;">Last Modification</span>
										<input class="form-control" type="text" name="xpath_timestamp" value="{{ old('xpath_timestamp',$source->xpath_timestamp) }}">
									</div>
									<div class="input-group">
										<span class="input-group-addon" style="min-width:200px;">Publication Title</span>
										<input class="form-control" type="text" name="xpath_title" value="{{ old('xpath_title',$source->xpath_title) }}">
									</div>	
									<div class="input-group">
										<span class="input-group-addon" style="min-width:200px;">Publication Unique ID</span>
										<input class="form-control" type="text" name="xpath_doi" value="{{ old('xpath_doi',$source->xpath_doi) }}">
									</div>	
									<div class="input-group">
										<span class="input-group-addon" style="min-width:200px;">Publication Authors</span>
										<input class="form-control" type="text" name="xpath_authors" value="{{ old('xpath_authors',$source->xpath_authors) }}">
									</div>	
									<div class="input-group">
										<span class="input-group-addon" style="min-width:200px;">Publication Abstract</span>
										<input class="form-control" type="text" name="xpath_abstract" value="{{ old('xpath_title',$source->xpath_abstract) }}">
									</div>	
									<div class="input-group">
										<span class="input-group-addon" style="min-width:200px;">Publication Year</span>
										<input class="form-control" type="text" name="xpath_year" value="{{ old('xpath_year',$source->xpath_year) }}">
									</div>									
									<div class="input-group">
										<span class="input-group-addon" style="min-width:200px;">Publication Type</span>
										<input class="form-control" type="text" name="xpath_type" value="{{ old('xpath_type',$source->xpath_type) }}">
									</div>	
									<div class="input-group">
										<span class="input-group-addon" style="min-width:200px;">Publication Faculty</span>
										<input class="form-control" type="text" name="xpath_faculty" value="{{ old('xpath_faculty',$source->xpath_faculty) }}">
									</div>	
									<div class="input-group">
										<span class="input-group-addon" style="min-width:200px;">Publication Programme</span>
										<input class="form-control" type="text" name="xpath_group" value="{{ old('xpath_group',$source->xpath_group) }}">
									</div>		
									<div class="input-group">
										<span class="input-group-addon" style="min-width:200px;">Publication PDF</span>
										<input class="form-control" type="text" name="xpath_pdf" value="{{ old('xpath_pdf',$source->xpath_pdf) }}">
									</div>										
								</div>
							</div>
						</div>						
					  </div>
					</div>
				@else
					<div class="box-body">
					  <div class="row">
						<div class="col-md-12">
						  <p class="text-center">
						  Please input the external source URL to continue.
						  </p>
							<input name="title" type="text" id="inputTitle" class="form-control" placeholder="Title" required autofocus>
							<input name="url" type="text" id="inputUrl" class="form-control" placeholder="http://" required>	
						</div>
					  </div>
					</div>					
				@endif
					<div class="box-footer clearfix">
						  <button class="btn btn-success btn-flat pull-right" type="submit">Save changes</button>
						   <a class="btn btn-info btn-flat pull-left" href="/source">Back to overview</a>
					</div>	
				</form>
			</div>
		</div>
@endsection