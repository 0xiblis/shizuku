package moe.shizuku.manager.wifi

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.R

class WifiAdapter(
    private val context: Context
) : RecyclerView.Adapter<WifiAdapter.ViewHolder>() {

    data class Item(
        val ssid: String,
        val password: String?,
        val security: String
    )

    private val items = ArrayList<Item>()
    private val revealed = HashSet<String>()

    fun setData(networks: List<Item>) {
        items.clear()
        items.addAll(networks)
        revealed.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.wifi_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val title =
            itemView.findViewById<TextView>(android.R.id.title)

        private val security =
            itemView.findViewById<TextView>(R.id.security)

        private val password =
            itemView.findViewById<TextView>(R.id.password)

        fun bind(item: Item) {
            title.text = item.ssid
            security.text = item.security

            updatePassword(item)

            // Tap = show/hide password
            itemView.setOnClickListener {
                if (item.password.isNullOrEmpty()) {
                    Toast.makeText(
                        context,
                        R.string.wifi_no_password,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                if (revealed.contains(item.ssid)) {
                    revealed.remove(item.ssid)
                } else {
                    revealed.add(item.ssid)
                }

                updatePassword(item)
            }

            // Long press = copy password
            itemView.setOnLongClickListener {
                val value = item.password

                if (value.isNullOrEmpty()) {
                    return@setOnLongClickListener false
                }

                val clipboard =
                    context.getSystemService(
                        Context.CLIPBOARD_SERVICE
                    ) as ClipboardManager

                clipboard.setPrimaryClip(
                    ClipData.newPlainText(
                        "WiFi password",
                        value
                    )
                )

                true
            }
        }

        private fun updatePassword(item: Item) {
            password.text = when {
                item.password.isNullOrEmpty() ->
                    context.getString(R.string.wifi_no_password)

                revealed.contains(item.ssid) ->
                    item.password

                else ->
                    "••••••••••••"
            }
        }
    }
}
