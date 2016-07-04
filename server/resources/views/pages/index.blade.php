@extends('app')
@section('content')
<div class="row">

		<div class="col-md-6 col-sm-6 col-xs-12">
		  <div class="info-box">
			<span class="info-box-icon bg-red"><i class="fa fa-bolt"></i></span>

			<div class="info-box-content">
			  <span class="info-box-text">Connected workers</span>
			  <span class="info-box-number">{{$workers}} worker{{($workers==1)?'':'s'}} are connected</span>
			</div>

		  </div>

		</div>

		<div class="col-md-6 col-sm-6 col-xs-12">
		  <div class="info-box">
			<span class="info-box-icon bg-aqua"><i class="fa fa-gear"></i></span>

			<div class="info-box-content">
			  <span class="info-box-text">Total worker CPU time</span>
			  <span class="info-box-number">{{$cputime}}</span>
			</div>

		  </div>

		</div>

</div>
<div class="row">

		<div class="col-md-6 col-sm-6 col-xs-12">
		  <div class="info-box">
			<span class="info-box-icon bg-green"><i class="fa fa-flag-checkered"></i></span>

			<div class="info-box-content">
			  <span class="info-box-text">Completed tasks</span>
			  <span class="info-box-number">{{number_format($completed)}}</span>
			</div>
			
		  </div>

		</div>

		<div class="col-md-6 col-sm-6 col-xs-12">
		  <div class="info-box">
			<span class="info-box-icon bg-yellow"><i class="fa fa-clock-o"></i></span>

			<div class="info-box-content">
			  <span class="info-box-text">Uncompleted tasks</span>
			  <span class="info-box-number">{{number_format($uncompleted)}}</span>
			</div>

		  </div>

		</div>

</div>
<div class="row">


          <!-- Info Boxes Style 2 -->
	<div class="col-md-6 col-sm-6 col-xs-12">
          <div class="info-box bg-yellow">
            <span class="info-box-icon"><i class="fa fa-database"></i></span>

            <div class="info-box-content">
              <span class="info-box-text">Documents in database</span>
              <span class="info-box-number">{{number_format($papers)}}</span>

              <div class="progress">
                <div class="progress-bar" style="width: 100%"></div>
              </div>
                  <span class="progress-description">
                    Entries in all repositories
                  </span>
            </div>
            <!-- /.info-box-content -->
          </div>
	</div>
	<div class="col-md-6 col-sm-6 col-xs-12">
          <!-- /.info-box -->
          <div class="info-box bg-green">
            <span class="info-box-icon"><i class="fa fa-check-square-o"></i></span>

            <div class="info-box-content">
              <span class="info-box-text">Journals extracted</span>
              <span class="info-box-number">{{number_format($journals)}}</span>

              <div class="progress">
                <div class="progress-bar" style="width: {{round(($journals/max(1,$papers))*100)}}%"></div>
              </div>
                  <span class="progress-description">
                    Entries from which journal references were extracted
                  </span>
            </div>
            <!-- /.info-box-content -->
          </div>
	</div>
</div>

@endsection