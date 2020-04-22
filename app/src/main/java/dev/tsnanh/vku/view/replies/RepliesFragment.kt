/*
 * Copyright (c) 2020 VKU by tsnAnh
 */

package dev.tsnanh.vku.view.replies

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkManager
import com.google.android.material.transition.MaterialContainerTransform
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dev.tsnanh.vku.R
import dev.tsnanh.vku.adapters.RepliesAdapter
import dev.tsnanh.vku.databinding.FragmentRepliesBinding
import dev.tsnanh.vku.network.NetworkPost
import dev.tsnanh.vku.utils.sendNotification
import dev.tsnanh.vku.view.replies.newreply.NewReplyFragment
import dev.tsnanh.vku.worker.POST
import timber.log.Timber

const val POST_TAG = "create_post"

class RepliesFragment : Fragment() {

    private lateinit var binding: FragmentRepliesBinding
    private lateinit var viewModel: RepliesViewModel
    private val navArgs: RepliesFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedElementEnterTransition = MaterialContainerTransform()
        sharedElementReturnTransition = MaterialContainerTransform()

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().navigateUp()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil
            .inflate(inflater, R.layout.fragment_replies, container, false)

        binding.bottomAppBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Timber.d(navArgs.threadId)
        binding.lifecycleOwner = viewLifecycleOwner
        viewModel = ViewModelProvider(
            this,
            RepliesViewModelFactory(navArgs.threadId, requireActivity().application)
        ).get(RepliesViewModel::class.java)

        val swipeController = SwipeController()
        val itemTouchHelper = ItemTouchHelper(swipeController)
        binding.listReplies.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            itemTouchHelper.attachToRecyclerView(this)
        }

        val adapter = RepliesAdapter()
        binding.listReplies.adapter = adapter

        viewModel.replies.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it)
            }
        })

        viewModel.createPostWorkerLiveData.observe(viewLifecycleOwner, Observer {
            if (it.isNullOrEmpty()) {
                return@Observer
            }
            val workInfo = it[0]

            if (workInfo.state.isFinished) {
                val jsonAdapter =
                    Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                        .adapter(NetworkPost::class.java)
                val json = workInfo.outputData.getString(POST)
                if (json != null && json.isNotEmpty()) {
                    val post = jsonAdapter.fromJson(json)
                    post?.let {
                        (requireContext()
                            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                            .sendNotification(
                                post.content,
                                "${post.content} is successfully created!",
                                requireContext().applicationContext
                            )
                    }
                }
                WorkManager.getInstance(requireContext()).pruneWork()
                viewModel.refresh()
            }
        })

        binding.fabReply.setOnClickListener {
            openNewReplyDialog()
        }
    }

    fun openNewReplyDialog(quotedPostId: String = "") {
        val sheet = NewReplyFragment(navArgs.threadId, navArgs.threadTitle, quotedPostId)
        sheet.setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialogStyle)
        sheet.show(childFragmentManager, TAG_BOTTOM_SHEET)
    }
}

private const val TAG_BOTTOM_SHEET = "new_reply"