package com.ljuntyg.gitnotes

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.ljuntyg.gitnotes.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var notesViewModel: NotesViewModel
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        val notesDao = NotesDatabase.getDatabase(this.applicationContext).notesDao()
        val notesRepository = NotesRepository(notesDao)
        val notesViewModelFactory = NotesViewModelFactory(notesRepository)
        notesViewModel = ViewModelProvider(this, notesViewModelFactory)[NotesViewModel::class.java]

        // Handle sharing of text from external application
        val intent = intent
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                handleSendText(intent)
            }
        }

        /**for (i in 1..100) {
        notesViewModel.viewModelScope.launch {
        val note = Note(title = i.toString(), body = getString(R.string.lorem_ipsum))
        note.id = notesViewModel.insertAsync(note).await()
        }
        }*/

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                val action =
                    RecyclerViewFragmentDirections.actionRecyclerViewFragmentToSettingsFragment()
                navController.navigate(action)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    private fun handleSendText(intent: Intent) {
        val sharedText =
            intent.getStringExtra(Intent.EXTRA_TEXT) ?: "ERROR: No text from intent found"
        val longTitle = sharedText.split("\n").first()
        val maxLength = 250
        val title = if (longTitle.length <= maxLength) {
            longTitle
        } else {
            "${longTitle.take(maxLength - 3)}..."
        }

        val newNote = Note(title = title, body = sharedText)
        notesViewModel.viewModelScope.launch {
            newNote.id = notesViewModel.insertAsync(newNote).await()
            val action = RecyclerViewFragmentDirections.actionRecyclerViewFragmentToNoteFragment(
                newNote,
                true
            )
            navController.navigate(action)
        }
    }
}