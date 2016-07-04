<?php

use Illuminate\Database\Seeder;

class UsersTableSeeder extends Seeder
{
    /**
     * Create first system user
     *
     * @return void
     */
    public function run()
    {
        DB::table('users')->insert([
            'name' => 'CiteRep Admin',
            'email' => 'admin@citerep.nl',
            'password' => bcrypt('citerep'),
        ]);
    }
}
