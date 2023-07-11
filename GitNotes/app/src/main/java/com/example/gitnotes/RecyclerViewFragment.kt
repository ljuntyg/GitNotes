package com.example.gitnotes

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gitnotes.databinding.FragmentRecyclerViewBinding
import com.google.android.material.snackbar.Snackbar

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class RecyclerViewFragment : Fragment() {

    private var _binding: FragmentRecyclerViewBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentRecyclerViewBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Notes test
        val note1 = Note(null, "titleTest", "body test");
        val note2 = Note(null, "another title", "BODY");
        val note3 = Note(null, "title3", "testBody");
        val noteList = listOf<Note>(note1, note2, note3);

        val adapter = NoteListAdapter(noteList)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        // Set up FAB click listener
        binding.fab.setOnClickListener { fabView ->
            Snackbar.make(fabView, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAnchorView(binding.fab)
                .setAction("Action", null).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}