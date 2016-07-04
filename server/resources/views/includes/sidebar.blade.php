<!-- Left side column. contains the logo and sidebar -->
      <aside class="main-sidebar">
        <!-- sidebar: style can be found in sidebar.less -->
        <section class="sidebar">
          <!-- search form -->
          <form action="/publication" method="post" class="sidebar-form">
		    {!! csrf_field() !!}
            <div class="input-group">
              <input type="text" name="title" class="form-control" placeholder="Search...">
              <span class="input-group-btn">
                <button type="submit" name="search" id="search-btn" class="btn btn-flat"><i class="fa fa-search"></i></button>
              </span>
            </div>
          </form>
          <!-- /.search form -->
          <!-- sidebar menu: : style can be found in sidebar.less -->
          <ul class="sidebar-menu">

            <li>
              <a href="/">
                <i class="fa fa-home"></i> <span>Home</span>
              </a>
            </li>
            <li>
              <a href="/source">
                <i class="fa fa-external-link-square"></i> <span>Sources</span>
              </a>
            </li>
            <li>
              <a href="/publication">
                <i class="fa fa-edit"></i> <span>Publications</span>
              </a>
            </li>	
            <li>
              <a href="/reports">
                <i class="fa fa-bar-chart"></i> <span>Reports</span>
              </a>
            </li>				
            <li>
				<a href="/workers">
					<i class="fa fa-link"></i> <span>Workers</span>
				</a>
			</li>
          </ul>
        </section>
        <!-- /.sidebar -->
      </aside>