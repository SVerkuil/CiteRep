<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
	<meta name="robots" content="noindex,nofollow" />
    <title>CitRep</title>
    <meta content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no' name='viewport'>

    <!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
        <script src="https://oss.maxcdn.com/html5shiv/3.7.3/html5shiv.min.js"></script>
        <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->
  </head>
	  <body class="hold-transition skin-blue sidebar-mini">
		<div class="wrapper">
            @include('includes.header')
            @include('includes.sidebar')
            <!-- Content Wrapper. Contains page content -->
            <div class="content-wrapper">
                <!-- Content Header (Page header) -->
                <section class="content-header">
                    <h1>
                    {{ isset($title)?$title:"" }}
                    </h1>
                </section>
                <!-- Main content -->
                <section class="content">
					<div class="row">
						<div class="col-xs-12">
							@yield('content')
						</div>
					</div>
                </section><!-- /.content -->
            </div><!-- /.content-wrapper -->
			@include('includes.footer')
        </div><!-- ./wrapper -->
 
		<!-- Include styles -->
		<link href="{{ asset('/bootstrap/css/bootstrap.min.css') }}" rel="stylesheet" type="text/css" />    
		<link href="https://maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css" rel="stylesheet" type="text/css" />
		<link href="https://code.ionicframework.com/ionicons/2.0.0/css/ionicons.min.css" rel="stylesheet" type="text/css" />    
		<link href="{{ asset('/dist/css/skins/_all-skins.min.css') }}" rel="stylesheet" type="text/css" />
		<link href="{{ asset('/plugins/iCheck/flat/blue.css') }}" rel="stylesheet" type="text/css" />
		<link href="{{ asset('/plugins/morris/morris.css') }}" rel="stylesheet" type="text/css" />
		<link href="{{ asset('/plugins/jvectormap/jquery-jvectormap-1.2.2.css') }}" rel="stylesheet" type="text/css" />
		<link href="{{ asset('/plugins/datepicker/datepicker3.css') }}" rel="stylesheet" type="text/css" />
		<link href="{{ asset('/plugins/daterangepicker/daterangepicker-bs3.css') }}" rel="stylesheet" type="text/css" />
		<link href="{{ asset('/plugins/bootstrap-wysihtml5/bootstrap3-wysihtml5.min.css') }}" rel="stylesheet" type="text/css" />
		<link href="{{ asset('/plugins/bootstrap-slider/slider.css') }}" rel="stylesheet" type="text/css" />
		<link href="{{ asset('/plugins/select2/select2.min.css') }}" rel="stylesheet" type="text/css" />
		<link href="{{ asset('/dist/css/AdminLTE.min.css') }}" rel="stylesheet" type="text/css" />
		@yield('styles')
		
		<!-- Include scripts -->
		<script src="{{ asset('/plugins/jQuery/jQuery-2.1.4.min.js') }}"></script>
		<script src="{{ asset('/plugins/moment/moment.min.js') }}"></script>
		<script src="https://code.jquery.com/ui/1.11.4/jquery-ui.min.js"></script>
		<script> $.widget.bridge('uibutton', $.ui.button);</script>
		<script src="{{ asset('/bootstrap/js/bootstrap.min.js') }}" type="text/javascript"></script>  
		<script src="https://cdnjs.cloudflare.com/ajax/libs/raphael/2.1.0/raphael-min.js"></script>
		<script src="{{ asset('/plugins/morris/morris.min.js') }}" type="text/javascript"></script>
		<script src="{{ asset('/plugins/sparkline/jquery.sparkline.min.js') }}" type="text/javascript"></script>
		<script src="{{ asset('/plugins/jvectormap/jquery-jvectormap-1.2.2.min.js') }}" type="text/javascript"></script>
		<script src="{{ asset('/plugins/jvectormap/jquery-jvectormap-world-mill-en.js') }}" type="text/javascript"></script>
		<script src="{{ asset('/plugins/knob/jquery.knob.js') }}" type="text/javascript"></script>
		<script src="{{ asset('/plugins/daterangepicker/daterangepicker.js') }}" type="text/javascript"></script>
		<script src="{{ asset('/plugins/datepicker/bootstrap-datepicker.js') }}" type="text/javascript"></script>
		<script src="{{ asset('/plugins/bootstrap-wysihtml5/bootstrap3-wysihtml5.all.min.js') }}" type="text/javascript"></script>
		<script src="{{ asset('/plugins/iCheck/icheck.min.js') }}" type="text/javascript"></script>
		<script src="{{ asset('/plugins/slimScroll/jquery.slimscroll.min.js') }}" type="text/javascript"></script>
		<script src="{{ asset('/plugins/fastclick/fastclick.min.js') }}"></script>
		<script src="{{ asset('/plugins/bootstrap-slider/bootstrap-slider.js') }}"></script>
		<script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.1.6/Chart.min.js"></script>
		<script src="{{ asset('/plugins/select2/select2.full.min.js') }}"></script>
		<script src="{{ asset('/dist/js/app.min.js') }}" type="text/javascript"></script>
		@yield('scripts')
    </body>
</html>