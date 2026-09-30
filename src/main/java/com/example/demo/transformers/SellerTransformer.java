package com.example.demo.transformers;

import com.example.demo.dto.request.SellerRequest;
import com.example.demo.dto.response.SellerResponse;
import com.example.demo.model.Seller;

import java.util.ArrayList;

public class SellerTransformer {

    //Seller Request to Seller
    public static Seller sellerRequestToSeller(SellerRequest request){
        return Seller.builder()
                .name(request.getName())
                .email(request.getEmail())
                .pan(request.getPan())
                .build();
    }

    //Seller to SellerResponse
    public static SellerResponse sellerToSellerResponse(Seller seller){
        return SellerResponse.builder()
                .name(seller.getName())
                .email(seller.getEmail())
                .build();
    }
}
