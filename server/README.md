This folder contains all files associated with the PHP CiteRep server

REQUIREMENTS
============
* PHP >=5.5.9 (PHP 7 preferred)
* Apache >= 2.4 with openssl and rewrite modules

INSTALLATION
============

Als eerste moet je een omgeving hebben waar Apache, PHP7 en MySQL op draait.
Op windows gebruik je daar een standaard pakket voor: http://www.wampserver.com/en/ (installeren, en klaar)
Voor linux zijn er packages die je eenvoudig kunt installeren 
Google eens op install LAMP, LAMP staat voor linux apache mysql php voor je distributie
Zie bijvoorbeeld https://www.digitalocean.com/community/tutorials/how-to-install-linux-apache-mysql-php-lamp-stack-on-ubuntu-16-04
Let er even op dat je de laatste versie van PHP binnenhaalt (PHP7)
Je kunt ook een oude PHP updaten naar PHP7 (https://www.digitalocean.com/community/tutorials/how-to-upgrade-to-php-7-on-ubuntu-14-04)
Check even of je je webserver kunt benaderen vanuit je browser, plaatst een documentje in de public directory (google even welke dat is, meestal /var/www op linux of wamp/www op windows) en kijk of je erbij kunt (door ip van je machine in firefox in te tikken)
Optioneel kun je een domeinnaam aan het IP adres van je webserver koppelen, dat laat ik nu even in het midden (keywords: DNS, apache virtual host)
Je moet nu binnen MySQL een gebruiker aanmaken, dit is de tabel waar straks alle data in komt

Zoek uit wat je standaard mysql root wachtwoord is (meestal: root en wachtwoord leeg in WAMP)
Stel mysql zo in dat je er van buiten bij kunt (grant privileges..., google even), verander je root wachtwoord naar iets veiligs :)
Zelf gebruik ik altijd https://www.mysql.com/products/workbench/
Connect mysql workbench met je server ip, root user en wachtwoord
Je kunt nu in een gui omgeving gemakkelijk users toevoegen en tabellen aanmaken
maak een user (met pwd) en lege tabel voor CiteRep, die heb je zo nodig
Als je de webserver draaiend hebt, plaatst je de inhoud van de github server ergens op je computer (niet in de www map)

Installeer composer op je systeem https://getcomposer.org/download/
Zorg dat je composer eenvoudig kunt aanroepen via de commandline
Navigeer naar de map waar je de inhoud van de CiteRep server hebt geplaatst
Doe de stappen zoals hier beschreven staat: https://github.com/SVerkuil/CiteRep/tree/master/server (hier stel je ook de database voor citerep in)
Na het uitvoeren van "composer install" zie je dat alle dependencies gedownload zijn, en de mysql database bevat nu een tabel.
Alles is nu geinstaleerd, maar je kunt er nog niet bij (inhoud staat immers niet in de www-map).

Je kunt nu twee dingen doen. Als er maar 1 site op de server draait, kun je de apache directory veranderen naar de locatie waar je zojuist alle CiteRep server bestanden hebt neergezet, en dan specifiek de submap httpdocs. De config file van apache hangt af van je installatie, maar is meestal te vinden in /etc/apache2/conf/httpd.conf. Pas de Directory dan aan naar <Directory "/home/pad/naar/citerep/httpdocs">
Als er meerdere sites draaien, dan kun je een virtual host aanmaken zoals hier staat https://httpd.apache.org/docs/2.4/vhosts/examples.html, de Document Root is dan "/home/pad/naar/citerep/httpdocs" en als servername moet je iets ingeven (bijvoorbeeld localhost als je WAMP op je eigen computer hebt geinstallerd) wat resolved naar het IP van je machine. Je kunt ook, mocht je een domeinnaam hebben, een subdomein aanmaken (sub.mijndomein.nl) met een A record naar het ip van je machine. Je vult dan in bij Servername: sub.mijndomein.nl
Restart apache (service apache restart, of service apache2 restart, of /etc/init.d/apache restart, of wat er ook bij je distributie hoort, of gewoon WAMP aan uit op windows)
Als je dan nu naar je webserver gaat in je browser (localhost of sub.mijndomein.nl) dan laad apache op de achtergrond het bestand /home/pad/naar/citerep/httpdocs/index.php uit de Document Root. Die index.php (komt van Laravel Framework https://laravel.com) laad dan de rest van het framework en de functionaliteiten.

You can now navigate to your webserver in your browser and login
