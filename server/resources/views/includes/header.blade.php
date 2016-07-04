	<header class="main-header">
        <!-- Logo -->
        <a href="/" class="logo">
          <!-- mini logo for sidebar mini 50x50 pixels -->
          <span class="logo-mini" style="font-size:12px;"><b>CITE</b>REP</span>
          <!-- logo for regular state and mobile devices -->
          <span class="logo-lg"><b>CITATION</b>REPORTS</span>
        </a>
        <!-- Header Navbar: style can be found in header.less -->
        <nav class="navbar navbar-static-top" role="navigation">
          <!-- Sidebar toggle button-->
          <a href="#" class="sidebar-toggle" data-toggle="offcanvas" role="button">
            <span class="sr-only">Toggle navigation</span>
          </a>
          <div class="navbar-custom-menu">
            <ul class="nav navbar-nav">

              <!-- User Account: style can be found in dropdown.less -->
              <li class="dropdown user user-menu">
                <a href="#" class="dropdown-toggle" data-toggle="dropdown">
                  <span class="hidden-xs">{{{ isset(Auth::user()->name) ? Auth::user()->name : Auth::user()->email }}}</span>
                  <span class="visible-xs"><i class="fa fa-user"></i></span>				  
                </a>
                <ul class="dropdown-menu">
                  <!-- User image -->
                  <li class="user-header">
                    <img src="dist/img/user_icon.png" class="img-circle" alt="User Image">
                    <p>
                      {{{ Auth::user()->name }}}
                      <small>{{{ Auth::user()->email }}}</small>
                    </p>
                  </li>				
                  <!-- Menu Footer-->
                  <li class="user-footer">
                      <a href="/logout" class="btn btn-default btn-flat">Logout</a>
                  </li>
                </ul>
              </li>
            </ul>
          </div>
        </nav>
      </header>