package com.example.user.groceryapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import com.amazonaws.mobileconnectors.apigateway.ApiClientFactory;
import com.grocery.clientsdk.GroceryAssistClient;
import com.grocery.clientsdk.model.OfferResponse;

import java.util.List;

import javax.xml.transform.Result;

//import APIclasses.GroceryAssistClient;
//import APIclasses.model.OfferResponse;

public class Notify extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notify);

    }

    ApiClientFactory factory = new ApiClientFactory();

    // Create an instance of your SDK. Here, 'SimpleCalcClient.java' is the compiled java class for the SDK generated by API Gateway.
    final GroceryAssistClient client = factory.build(GroceryAssistClient.class);

    // Invoke a method:
    //   For the 'GET /?a=1&b=2&op=+' method exposed by the API, you can invoke it by calling the following SDK method:

    OfferResponse output = client.getOffersGet("user1");


}
