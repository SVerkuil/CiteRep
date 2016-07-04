<?php

use Illuminate\Database\Schema\Blueprint;
use Illuminate\Database\Migrations\Migration;

class CreateJournalsTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('journals', function (Blueprint $table) {
            $table->increments('id');
			$table->integer('paper_id');
			$table->string('journal');
			$table->string('normalized');
			
			//Foreign keys and indexes
			$table->foreign('paper_id')->references('id')->on('papers')->onDelete('cascade');
			$table->index('normalized');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
		Schema::drop('journals');
    }
}
