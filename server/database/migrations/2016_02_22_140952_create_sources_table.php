<?php

use Illuminate\Database\Schema\Blueprint;
use Illuminate\Database\Migrations\Migration;

class CreateSourcesTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('sources', function (Blueprint $table) {
            $table->increments('id');
			$table->text('url');
			$table->text('title');
			$table->text('xpath_timestamp');
			$table->text('xpath_title');
			$table->text('xpath_doi');
			$table->text('xpath_authors');
			$table->text('xpath_abstract');
			$table->text('xpath_type');
			$table->text('xpath_faculty');
			$table->text('xpath_group');
			$table->text('xpath_pdf');
			$table->text('xpath_year');
			$table->text('last_query');
            $table->timestamps();
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::drop('sources');
    }
}
