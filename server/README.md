This folder contains all files associated with the PHP CiteRep server

REQUIREMENTS
============
* PHP >=5.5.9 (PHP 7 preferred)
* Apache >= 2.4 with openssl and rewrite modules

INSTALLATION
============

Place the contents of this folder on your webserver. 

Bind the webserver to the httpdocs folder (which contains index.php).
Make sure only contents in this folder are accessible from the web!

Create an empty MySQL database with username and password.
Collation: utf8_unicode_ci

Configure .env with your database settings. 
Make sure you change APP_KEY to 32 random characters!

Edit database/seeds/UsersTableSeeder.php to include the user accounts you want to use.
Default login is "admin@citerep.nl" with password "citerep"

Run "composer install"
This will also fill your MySQL database

You can now navigate to your webserver in your browser and login