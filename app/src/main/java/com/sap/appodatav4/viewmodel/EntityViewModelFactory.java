package com.sap.appodatav4.viewmodel;

import android.app.Application;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import android.os.Parcelable;

import com.sap.appodatav4.viewmodel.orderitem.OrderItemViewModel;
import com.sap.appodatav4.viewmodel.order.OrderViewModel;


/**
 * Custom factory class, which can create view models for entity subsets, which are
 * reached from a parent entity through a navigation property.
 */
public class EntityViewModelFactory implements ViewModelProvider.Factory {

	// application class
    private Application application;
	// name of the navigation property
    private String navigationPropertyName;
	// parent entity
    private Parcelable entityData;

	/**
	 * Creates a factory class for entity view models created following a navigation link.
	 *
	 * @param application parent application
	 * @param navigationPropertyName name of the navigation link
	 * @param entityData parent entity
	 */
    public EntityViewModelFactory(Application application, String navigationPropertyName, Parcelable entityData) {
        this.application = application;
        this.navigationPropertyName = navigationPropertyName;
        this.entityData = entityData;
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        T retValue = null;
		switch(modelClass.getSimpleName()) {



			case "OrderItemViewModel":
				retValue = (T) new OrderItemViewModel(application, navigationPropertyName, entityData);
				break;
			case "OrderViewModel":
				retValue = (T) new OrderViewModel(application, navigationPropertyName, entityData);
				break;
		}
		return retValue;
	}
}