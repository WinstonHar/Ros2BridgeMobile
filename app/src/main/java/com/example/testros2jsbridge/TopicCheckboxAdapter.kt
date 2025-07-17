package com.example.testros2jsbridge

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView

class TopicCheckboxAdapter(
    private var topics: List<Pair<String, String>>,
    internal var subscribedTopics: Set<String>,
    private val onCheckedChange: (topic: String, type: String, isChecked: Boolean) -> Unit
) : RecyclerView.Adapter<TopicCheckboxAdapter.TopicViewHolder>() {

    fun updateTopicsAndSubscribed(newTopics: List<Pair<String, String>>, newSubscribed: Set<String>) {
        topics = newTopics
        subscribedTopics = newSubscribed
        notifyDataSetChanged()
    }
    fun setSubscribedTopics(newSet: Set<String>) {
        subscribedTopics = newSet
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_topic_checkbox, parent, false)
        return TopicViewHolder(view)
    }

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        val (topic, type) = topics[position]
        holder.checkBox.text = "$topic  [$type]"
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = subscribedTopics.contains(topic)
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            onCheckedChange(topic, type, isChecked)
        }
    }

    override fun getItemCount(): Int = topics.size

    fun updateTopics(newTopics: List<Pair<String, String>>) {
        topics = newTopics
        notifyDataSetChanged()
    }

    class TopicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_topic)
    }
}
