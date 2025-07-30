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

    /*
        input:    newTopics - List<Pair<String, String>>, newSubscribed - Set<String>
        output:   None
        remarks:  Updates the topics and subscribed topics, then refreshes the adapter.
    */
    fun updateTopicsAndSubscribed(newTopics: List<Pair<String, String>>, newSubscribed: Set<String>) {
        topics = newTopics
        subscribedTopics = newSubscribed
        notifyDataSetChanged()
    }
    /*
        input:    newSet - Set<String>
        output:   None
        remarks:  Sets the subscribed topics and refreshes the adapter.
    */
    fun setSubscribedTopics(newSet: Set<String>) {
        subscribedTopics = newSet
        notifyDataSetChanged()
    }

    /*
        input:    parent - ViewGroup, viewType - Int
        output:   TopicViewHolder
        remarks:  Inflates the item view and creates a TopicViewHolder.
    */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_topic_checkbox, parent, false)
        return TopicViewHolder(view)
    }

    /*
        input:    holder - TopicViewHolder, position - Int
        output:   None
        remarks:  Binds the topic and type to the checkbox and sets the checked change listener.
    */
    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        val (topic, type) = topics[position]
        holder.checkBox.text = "$topic  [$type]"
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = subscribedTopics.contains(topic)
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            onCheckedChange(topic, type, isChecked)
        }
    }

    /*
        input:    None
        output:   Int
        remarks:  Returns the number of topics in the adapter.
    */
    override fun getItemCount(): Int = topics.size

    /*
        input:    newTopics - List<Pair<String, String>>
        output:   None
        remarks:  Updates the topics list and refreshes the adapter.
    */
    fun updateTopics(newTopics: List<Pair<String, String>>) {
        topics = newTopics
        notifyDataSetChanged()
    }

    class TopicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_topic)
    }
}
