@extends('app')
@section('content')
	<div class="row">
		<div class="col-xs-12">
			<div class="box box-success">
				<div class="box-header with-border">
				  <h3 class="box-title">Instructions</h3>
				  <div class="box-tools pull-right">
					<button type="button" class="btn btn-box-tool" data-widget="collapse"><i class="fa fa-minus"></i>
					</button>
				  </div>
				</div>
				<div class="box-body">
				  <div class="row">
					<div class="col-md-12">
					  <p class="text-center">
						Remote sources can be connected to this web interface using the tool below. It is assumed that remote sources follow the Open Archives Initiative (OAI) standard as specified at <a href="https://www.openarchives.org/">https://www.openarchives.org/</a>. However the tool does allow some degree of flexibility to implement other XML like interfaces.
					  </p>
					</div>
				  </div>
				</div>
				<div class="box-footer clearfix">
					<p class="text-center">
						<a href="/source/create" class="btn btn-info btn-flat">Add source</a>
					</p>
				</div>					
			</div>
		</div>
	</div>
	<div class="row">
	@foreach (App\CitRep\Source::all() as $source)
		<div class="col-md-6 col-lg-4">
			<div class="box box-info">
				<div class="box-header with-border">
				  <h3 class="box-title">{{ $source->title() }}</h3>
				  <div class="box-tools pull-right">
					<button type="button" class="btn btn-box-tool" data-widget="collapse"><i class="fa fa-minus"></i>
					</button>
				  </div>
				</div>
				<div class="box-body" style="min-height:100px;">
				  <div class="row">
					<div class="col-md-12">
					  <p class="text-center">
						<?php echo $source->description(); ?>
					  </p>
					</div>
				  </div>
				</div>
				<div class="overlay" id="overlay-{{$source->id}}" style="display:none;">
				  <i class="fa fa-refresh fa-spin"></i>
				</div>				
				<div class="box-footer clearfix">
					  <a href="/source/edit/{{$source->id}}" class="btn btn-success btn-flat pull-right">Edit</a>
					  <a href="#parse" class="btn btn-info btn-flat pull-right parseBtn" data-id="{{$source->id}}" id="parse-{{$source->id}}">Parse</a>
					  <a href="/source/delete/{{$source->id}}" class="btn btn-danger btn-flat pull-right" onclick="return confirm('Are you sure to delete this source?');">Delete</a>
				</div>				
			</div>
		</div>
	@endforeach
	</div>
@endsection
@section('scripts')
	<script type="text/javascript">
	$(".parseBtn").each(function() {
		var resume = ''; var parsed = 0;
		$(this).click(function() {
			var id = parseInt($(this).attr('data-id'));
			function parseFeed(id) {
				$('#overlay-'+id).show();
				$.ajax({
				  type: "POST",
				  url: '/source/parse/'+id,
				  data: {_token:"{{csrf_token()}}",resume:resume},
				  success: function(json){
					  /* json.msg contains readable message
					  *  json.count contains count of processed items
					  *  json.resume contains resumptionToken for next request
					  */
					  if(json.msg!='OK') {
							alert(json.msg); 
							$('#overlay-'+id).hide();
					  } else {
							resume = json.resume; 
							parsed = parsed + parseInt(json.count);
							$('#parse-'+id).text('Parsed '+parsed+' changes');
							if(resume!='') {
								parseFeed(id);
							} else {
								resume = ''; parsed = 0;
								$('#overlay-'+id).hide();
							}
					  }
				  },
				  error: function(){
					  alert('Internal server error, please try again later'); $('#overlay-'+id).hide();
					  parseFeed(id);
				  }
				});
				return false;	
			} return parseFeed(id);	
		});
	});
	</script>
@endsection