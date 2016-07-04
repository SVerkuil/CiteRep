<div class="col-xs-12">
	<div class="chart">
		<canvas id="overviewChart" style="height:200px"></canvas>
	</div>
</div>
<div class="col-xs-12 col-lg-6 col-lg-offset-3">
<h3 style="text-align:center;">Most Popular</h3>
	<div class="nav-tabs-custom">
		<ul class="nav nav-tabs">
		@foreach($top25 as $name => $obj)
		  <li class="{{($name=='total')?'active':''}}"><a aria-expanded="true" href="#{{md5($name)}}" data-toggle="tab">{{ ucfirst($name) }}</a></li>
		@endforeach
		</ul>
		<div class="tab-content">
		@foreach($top25 as $name => $list)
		  <div class="tab-pane {{($name=='total')?'active':''}}" id="{{md5($name)}}">
			<ul class="products-list product-list-in-box">
				@foreach($list as $obj)
				<li class="item">
					<div class="product-img">
						<input type="text" data-readOnly="true" data-thickness="0.1" data-angleArc="250" class="knob" value="{{$obj['c']}}" data-min="0" data-max="{{$obj['total']}}" data-width="50" data-angleoffset="-125" style="font-size:12px;" data-height="50" data-fgColor="#00c0ef" />
					</div>
					<div class="product-info">
						<span class="sparkline pull-right" style="margin-right:20px;">{{ $obj['pie'] }}</span>
						<span class="product-description">
						<b>{{ $obj['n'] }}</b>
						@foreach($obj['j'] as $j)
						<br />{{ucfirst($j)}}
						@endforeach
						</span>
					</div>
				</li>
				@endforeach
			</ul>
		  </div>	
		@endforeach		  
		</div>
	  </div>
</div>
<script type="text/javascript">
//Render knobs with counters
$(".knob").knob();

//Initialize sparkline
$(".sparkline").each(function () {
	  var $this = $(this);
	  $this.sparkline('html', {
		type: "pie",
		width: 50,
		height:50,
		
		tooltipFormat: '\{\{offset:offset\}\}<br/>(\{\{value\}\})',
		tooltipValueLookups: {
			'offset': {!! json_encode($faculties, JSON_FORCE_OBJECT) !!}
		},
	});
});

//Render charts on tab load
$('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
	$.sparkline_display_visible();
});

var chartOptions = {
  legend: {
	  display:false
  }
};	
	
	//Intialize chart
    var ChartCanvas = $("#overviewChart").get(0).getContext("2d");
    var CiteRepChart = new Chart(ChartCanvas, {
		type: 'bar',
		data: {
			labels: {!! json_encode($years) !!},
			datasets: {!! json_encode($chart) !!}
		},
		options: chartOptions
	});
</script>