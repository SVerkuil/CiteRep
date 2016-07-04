@extends('app')
@section('content')
<h2>Connection Info</h2>
<div class="row">
	<div class="col-sm-12 col-md-8 col-md-offset-2">
		<div class="box">
			<div class="box-header with-border">
			  <h3 class="box-title">Workers</h3>
			  <div class="box-tools pull-right">
				<button type="button" class="btn btn-box-tool" data-widget="collapse"><i class="fa fa-minus"></i>
				</button>
			  </div>
			</div>
			<!-- /.box-header -->
			<div class="box-body">
			  <div class="row">
				<div class="col-sm-12">
				  <p class="text-center">
					Workers need to be connected in order to perform tasks such as downloading and processing PDF files, reference extraction and citation normalization. Results produced by workers are visible in this web interface. If a worker is not connected when a task is created, the task will hold till a worker becomes available. Multiple workers can be connected at once using the credentials provided below.
				  </p>
				  <h2 class="text-center"></h2>
				  <p class="text-center">
					<div class="small-box bg-green">
						<div class="inner">
						  <h3>Endpoint</h3>
						  <p>{{ Request::url() }}</p>
						</div>
						<div class="icon">
						  <i class="fa fa-link"></i>
						</div>
					</div>
				  </p>
				  <h2 class="text-center"></h2>
					<div class="small-box bg-aqua">
						<div class="inner">
						  <h3>Passphrase</h3>
						  <p>{{ App\CitRep\Worker::passphrase() }}</p>
						</div>
						<div class="icon">
						  <i class="fa fa-lock"></i>
						</div>
					</div>
				</div>
			  </div>
			</div>
			<div class="box-footer clearfix">
				<p class="text-center">
					<a href="{{ App\CitRep\Worker::downloadURI() }}" class="btn btn-info btn-flat" target="_blank">Download Worker</a>
				</p>
			</div>
		</div>
	</div>
</div>
<h2>Connected Workers</h2>
<div class="row">
	@foreach (App\CitRep\Worker::getConnected() as $worker)
		<div class="col-md-6 col-lg-4">
			<?php echo $worker->dump(); ?>
		</div>
	@endforeach
</div>
@endsection