package com.team6.travel_app.adapter


import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.team6.travel_app.R
import com.team6.travel_app.data.Cart
import com.team6.travel_app.data.CartDatabase
import com.team6.travel_app.data.ImageDatabase
import com.team6.travel_app.data.ProductDatabase
import com.team6.travel_app.databinding.EachCartBinding
import com.team6.travel_app.databinding.FragmentCartBinding
import com.team6.travel_app.model.Product
import com.team6.travel_app.utils.downloadFromUrl
import com.team6.travel_app.utils.placeholderProgressBar
import com.team6.travel_app.view.PaymentActivity
import com.team6.travel_app.view.ProductDetailsActivity
import com.team6.travel_app.viewmodel.CartViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.round

class CartAdapter(
    private val cartList: ArrayList<Cart>,
    val context: Context,
    val database: CartDatabase,
    val bd: FragmentCartBinding,
) : RecyclerView.Adapter<CartAdapter.PlaceHolder>() {

    interface Listener {
        fun onItemClick(products: Product)//service : Service de alabilir.
    }
    class PlaceHolder(val binding: EachCartBinding) : RecyclerView.ViewHolder(binding.root) {
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PlaceHolder {
        val inflater = LayoutInflater.from(parent.context)
        //val binding = EachCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val binding =
            DataBindingUtil.inflate<EachCartBinding>(inflater, R.layout.each_cart, parent, false)
        return PlaceHolder(binding)
    }



    override fun onBindViewHolder(
        holder: PlaceHolder,
        position: Int
    ) {
        val viewModel = CartViewModel()
        holder.binding.product = cartList[position]
        holder.binding.imageOfProduct.downloadFromUrl(
            cartList[position].thumbnail,
            placeholderProgressBar(holder.itemView.context)
        )
        holder.binding.textViewOriginalPrice.paintFlags = holder.binding.textViewOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

        val discountedPrice = cartList[position].price!! * (100 - cartList[position].discountPercentage!!) / 100
        val roundedDiscountPrice = round(discountedPrice).toInt()
        holder.binding.textViewDiscountPrice.text = "$roundedDiscountPrice đ"
        // tru - va cap nhat gia
        holder.binding.productQuantityMinus.setOnClickListener {
            println("minus")
            changeProductQuantity(false, holder)
        }
        if (cartList[position].isDeposited == 1){
            holder.binding.depositButton.text = "Đã cọc"  // Update button text to show it's deposited
            holder.binding.depositButton.isEnabled = false  // Disable button after action
        }

        holder.binding.depositButton.setOnClickListener {
            holder.binding.depositButton.isEnabled = false  // Disable button to prevent multiple clicks
            CoroutineScope(Dispatchers.IO).launch {
                val position = holder.adapterPosition
                val cartItem = cartList[position]
                val cardDb = CartDatabase.invoke(context).cartDao()
                cartItem.id?.let { id ->
                    cardDb.setIsDeposited(id, 1)
                }
                val amount = cartItem.discountPercentage!!.toDouble() * cartItem.price!!.toInt() * 0.1
                withContext(Dispatchers.Main) {
                    val intent = Intent(context, PaymentActivity::class.java).apply {
                        putExtra("amount", round(amount).toInt())
                    }
                    context.startActivity(intent)
                    holder.binding.depositButton.text = "Đã cọc"
                    holder.binding.depositButton.isEnabled = false
                }
                viewModel.postRequestDeposit(context, database, cartList[position].id!!)
            }
        }
        holder.itemView.setOnClickListener {
            val id = cartList[position].id
            CoroutineScope(Dispatchers.IO).launch() {
                val images = ImageDatabase(context = context).imageDao().getRecord(id!!)
                val item = ProductDatabase(context).productDao().getRecord(id)
                val product1 = Product(item.id,item.title,item.description,item.price,item.discountPercentage,item.rating,item.stock,item.brand,item.category,item.thumbnail,images)
                Log.i(TAG, "onViewClicked: the value of product is : $product1")
                val intent = Intent(context, ProductDetailsActivity::class.java)
                intent.putExtra("product", product1)
                context.startActivity(intent)
            }
            Log.i(TAG, "onBindViewHolder: ${cartList[position].id}")
//            val viewModel = CartViewModel()
            viewModel.onViewClicked(context, cartList[position].id!!) // TODO viewmodel isn't triggered
        }
        holder.binding.productQuantityPlus.setOnClickListener {
            println("plus")
            changeProductQuantity(true, holder)

        }



    }


    override fun getItemCount(): Int {
        println("SIZE " + cartList.size)
        return cartList.size ?: 0
    }

    fun deleteItem(position: Int) {
        cartList.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, cartList.size)
    }

    fun getItemInfo(position: Int): Int? {
        return cartList[position].id
    }

    private fun changeProductQuantity(increaseQuantity: Boolean, holder: PlaceHolder) {
        val viewModel = CartViewModel()
        holder.binding.product?.id?.let { viewModel.updateQuantity(increaseQuantity,it,database,holder, bd) }

    }

    fun onQuantityTextChanged(text: CharSequence, cart: Cart, priceTextView: TextView) {
        val quantity = text.toString()
        if (quantity.isNotEmpty()) {
            val quantityNumber = quantity.toDouble().toInt()
        }
    }
}