@extends('app')
@section('content')
<div class="row">
	<form id="form" method="POST" action="">
	{!! csrf_field() !!}
        <div class="col-lg-3 col-md-6 col-xs-12">
          <!-- small box -->
          <div class="small-box bg-aqua">
            <div class="inner" style="height:320px;overflow:auto;">
              <h3>Basic</h3>
			  <div class="form-group">
				  <label>Report </label>
                  <select class="form-control" name="timespan">
                    <option value="1">1 year</option>
                    <option value="2">2 years</option>
                    <option value="3">3 years</option>
                    <option value="4">4 years</option>
                    <option value="5">5 years</option>
                  </select>
              </div>
			  <div class="form-group">
				  <label>Starting from </label>
                  <select class="form-control" name="year">
				  @foreach ($years as $obj)
					<option value="{{$obj->year}}">{{$obj->year}}</option>
				  @endforeach
                  </select>
              </div>
				<div class="form-group">
					<label>Including these sources</label>
					<select class="form-control select2" name="source[]" multiple="multiple" style="width: 100%;">
					@foreach ($sources as $source)
						<option value="{{$source->id}}" selected="selected">{{$source->title}}</option>
					@endforeach</select>	
				</div>
            </div>
            <div class="small-box-footer">Timespan &amp; Repositories</div>
          </div>
        </div>
        <!-- ./col -->
        <div class="col-lg-3 col-md-6 col-xs-12">
          <!-- small box -->
          <div class="small-box bg-green">
            <div class="inner" style="height:320px;overflow:auto;">
				<h3>Faculties</h3>
					<div class="form-group">
						<label>Leave empty to include all faculties</label>
						<select class="form-control select2" name="faculty[]" multiple="multiple" style="width: 100%;">
						@foreach ($faculties as $obj)
							<option value="{{$obj->faculty}}">{{$obj->faculty?$obj->faculty:"Other / Unknown"}}</option>
						@endforeach</select>
					</div>
            </div>
            <div class="small-box-footer">Restrict to these faculties</div>
          </div>
        </div>
        <!-- ./col -->
        <div class="col-lg-3 col-md-6 col-xs-12">
          <!-- small box -->
          <div class="small-box bg-yellow">
            <div class="inner" style="height:320px;overflow:auto;">
				<h3>Studies</h3>
					<div class="form-group">
						<label>Leave empty to include all studies</label>
						<select class="form-control select2" name="study[]" multiple="multiple" style="width: 100%;">
						@foreach ($studies as $obj)
							<option value="{{$obj->study}}">{{$obj->study?$obj->study:"Other / Unknown"}}</option>
						@endforeach</select>
					</div>
            </div>
            <div class="small-box-footer">Restrict to these studies</div>
          </div>
        </div>
        <!-- ./col -->
        <div class="col-lg-3 col-md-6 col-xs-12">
          <!-- small box -->
          <div class="small-box bg-red">
            <div class="inner" style="height:320px;overflow:auto;">
              <h3>Journals</h3>
              <div class="form-group">
                <label>Leave empty to include all journals</label>
                <select class="form-control select2" name="journal[]" multiple="multiple" style="width: 100%;">
					@foreach ($journals as $obj)
						<option value="{{$obj->normalized}}">{{$obj->journal}}</option>
					@endforeach</select>
                </select>
              </div>
            </div>
            <div class="small-box-footer">Restrict to these journals</div>
          </div>
        </div>
		<input type="hidden" name="download" id="dl" value="0" />
	</form>
</div>
<div class="row">
	<div class="col-xs-12 col-md-3 col-md-offset-3">
		<button class="btn btn-primary" style="width:100%;" id="search">Display Report</button>
	</div>
	<div class="col-xs-12 col-md-3">
		<button class="btn btn-default" style="width:100%;" id="download">Download Report</button>
	</div>
</div>
<div class="row" style="margin-top:20px;" id="report">

</div>
@endsection
@section('scripts')
	<script type="text/javascript">
    $(".select2").select2();
	$("#search").click(function() {
		$('#dl').val('0');
		$('#report').html('<div class="col-xs-12" style="text-align:center;font-size:100px;"><i class="fa fa-refresh fa-spin"></i></div>');
		
		//Serialize form
		var serialized = $("#form").serialize();
		$.post('?nocache='+(new Date()).getTime(),serialized,function(html) {
			$('#report').html(html);
		});
		
	});
	$("#download").click(function() {
		$('#dl').val('1');
		$('#form').submit();
	});
	</script>
@endsection
@section('styles')
<style type="text/css">
.nav-stacked > li {
	border-bottom: 1px solid #f4f4f4;
	margin: 0;
}
.knob {
	font-size: 12px !important;
}
.select2-search__field:focus {
	border: none !important;
}
</style>
@endsection