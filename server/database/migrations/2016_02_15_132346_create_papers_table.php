<?php

use Illuminate\Database\Schema\Blueprint;
use Illuminate\Database\Migrations\Migration;

class CreatePapersTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('papers', function (Blueprint $table) {
            $table->increments('id');
			$table->text('title');
			$table->string('doi')->unique();
			$table->text('authors');
			$table->text('abstract');
			$table->integer('year');
			$table->string('type');
			$table->string('faculty');
			$table->string('study');
			$table->text('pdf_url');
			$table->mediumText('pdf_plain');
			$table->mediumText('citations');
            $table->timestamps();
			$table->string('timestamp');
			$table->integer('source_id');
			$table->integer('status');
			$table->index('year');
			$table->index('faculty');
			$table->index('study');
			$table->index('source_id');
			$table->index('status');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::drop('papers');
    }
}
