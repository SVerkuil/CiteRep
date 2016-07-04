@extends('app')
@section('content')
	<div class="row">
		<div class="col-sm-12 col-md-6">
			<div class="box box-success" style="min-height:400px;">
				<form method="POST" action="?page=1" id="searchForm">
				{!! csrf_field() !!}
				<div class="box-header with-border">
				  <h3 class="box-title">Filters</h3>
				</div>
				<div class="box-body">
				  <div class="row">
					<div class="col-xs-12">
						
						<table class="table table-striped">
							<tr>
								<th style="width:80px;">Year</th>						
								<td style="padding-right:15px;cursor:pointer;"><input type="text" name="years" value="" class="slider form-control" data-slider-min="{{$year_min}}" data-slider-max="{{date('Y')}}" data-slider-step="1" data-slider-value="[{{$search_years}}]" data-slider-orientation="horizontal" data-slider-selection="before" data-slider-tooltip="show" data-slider-id="blue"></td>
							</tr>
							<tr>
								<th style="width:80px;">Sources</th>
								<td><div class="form-group">@foreach ($sources as $source)
								  <div class="checkbox">
									<label>
									  <input type="checkbox" name="sources[{{$source->id}}]" value="{{$source->id}}" {{ in_array($source->id,$search_sources)?"checked='checked'":"" }} />
									   <i class="fa fa-circle" style="color:{{$source->getColor()}};"></i> {{ $source->title }}
									</label>
								  </div>
								@endforeach</div></td>
							</tr>
							<tr>
								<th style="width:80px;">Search</th>						
								<td><input type="text" name="title" value="{{$search_title}}" class="form-control" placeholder="Search Title"></td>
							</tr>
							
						</table>
						
					</div>
				  </div>
				</div>
				<div class="box-footer clearfix">			
					<p class="text-center">
						<input type="submit" name="send" value="New Search" class="btn btn-info btn-flat" />
					</p>
					{!! $papers->render() !!}
				</div>		
				</form>
			</div>
		</div>
		<div class="col-sm-12 col-md-6">
			<div class="box box-success">
				<div class="box-header with-border">
				  <h3 class="box-title">Publications per Source</h3>				  
				</div>
				<div class="box-body">
					  <div class="chart">
						<canvas id="yearChart" style="height:120px"></canvas>
					  </div>
				</div>			
			</div>
			<div class="box box-success">
				<div class="box-header with-border">
				  <h3 class="box-title">Publications per Programme</h3>				  
				</div>
				<div class="box-body">
					  <div class="chart">
						<canvas id="programmeChart" style="height:120px"></canvas>
					  </div>
				</div>			
			</div>			
		</div>	
	</div>
	<div class="row">
	@foreach ($papers as $i => $paper)
		@if ($i%2==0)
			</div><div class="row">
		@endif	
		<div class="col-sm-6">
		<?php $paper->dump(false); ?>
		</div>
	@endforeach
	</div>
@endsection
@section('scripts')
	<script type="text/javascript">
	
	//Fix pagination, use POST
	$('.pagination li a').click(function() {
		$('#searchForm').attr('action',$(this).attr('href'));
		$('#searchForm').submit();
		return false;
	});
	
	
	//Initialize sliders
	$('.slider').bootstrapSlider();
	
	//Chart configuration
	var	chartOptions = {
	  maintainAspectRatio: false,
	  legend: {
		  display:false
	  }
	};		
	
	//Intialize linechart
    var lineChartCanvas = $("#yearChart").get(0).getContext("2d");
    var lineChart = new Chart(lineChartCanvas,{
		type: 'line',
		data: {!! $js_linechart !!},
		options: chartOptions
	});
	

   //Initialize barchart
    var barChartCanvas = $("#programmeChart").get(0).getContext("2d");
    var barChart = new Chart(barChartCanvas,{
		type: 'bar',
		data: {!! $js_barchart !!},
		options: chartOptions
	});	
	</script>
@endsection