package com.sap.appodatav4.viewmodel.orderitem;

import android.app.Application;
import android.os.Parcelable;

import com.sap.appodatav4.viewmodel.EntityViewModel;
import com.sap.cloud.android.odata.container.OrderItem;
import com.sap.cloud.android.odata.container.ContainerMetadata.EntitySets;

/*
 * Represents View model for OrderItem
 * Having an entity view model for each <T> allows the ViewModelProvider to cache and
 * return the view model of that type. This is because the ViewModelStore of
 * ViewModelProvider cannot not be able to tell the difference between EntityViewModel<type1>
 * and EntityViewModel<type2>.
 */
public class OrderItemViewModel extends EntityViewModel<OrderItem> {

    /**
    * Default constructor for a specific view model.
    * @param application - parent application
    */
    public OrderItemViewModel(Application application) {
        super(application, EntitySets.orderItemSet, OrderItem.itemID);
    }

    /**
    * Constructor for a specific view model with navigation data.
    * @param application - parent application
    * @param navigationPropertyName - name of the navigation property
    * @param entityData - parent entity (starting point of the navigation)
    */
	 public OrderItemViewModel(Application application, String navigationPropertyName, Parcelable entityData) {
        super(application, EntitySets.orderItemSet, OrderItem.itemID, navigationPropertyName, entityData);
    }
}
