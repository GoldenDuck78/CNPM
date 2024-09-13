package com.team6.travel_app.view

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.team6.travel_app.adapter.CategorizedProduct
import com.team6.travel_app.databinding.ActivityCategoryBinding
import com.team6.travel_app.model.Product
import com.team6.travel_app.service.ProductsAPI
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class CategoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCategoryBinding
    private lateinit var categorizedProduct: CategorizedProduct
    private var productList = ArrayList<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize RecyclerView with GridLayoutManager
        val layoutManager = GridLayoutManager(this, 2) // Changed to 2 columns
        binding.recyclerViewCategorizedProduct.layoutManager = layoutManager

        // Set the action bar title
        val categoryType = intent.getStringExtra("category_type")
        supportActionBar?.title = categoryType

        // Prepare Retrofit to make API calls
        val retrofit = Retrofit.Builder()
            .baseUrl(MainActivity.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Create API service
        val service = retrofit.create(ProductsAPI::class.java)

        // Construct the URL for categorized products
        val url = "products/category/$categoryType"

        // Make API call
        service.getCategorizedProduct(url)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())  // Observe the result on the main thread
            .subscribe({ responseBody ->
                val json = responseBody.string()
                val gson = Gson()
                val productType = object : TypeToken<List<Product>>() {}.type
                val products: List<Product> = gson.fromJson(json, productType)
                // Update the product list and UI
                productList.addAll(products)
                categorizedProduct = CategorizedProduct(productList, this@CategoryActivity)
                binding.recyclerViewCategorizedProduct.adapter = categorizedProduct
            }, { throwable ->
                // Handle the error case
                throwable.printStackTrace()
                // You can show a Toast message or any other error indication here
            })

    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu if needed
        return super.onCreateOptionsMenu(menu)
    }
}
