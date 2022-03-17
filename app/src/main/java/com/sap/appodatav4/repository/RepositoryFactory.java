package com.sap.appodatav4.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sap.cloud.android.odata.container.Container;
import com.sap.cloud.android.odata.container.ContainerMetadata.EntitySets;

import com.sap.cloud.android.odata.container.OrderItem;
import com.sap.cloud.android.odata.container.Order;

import com.sap.cloud.mobile.odata.EntitySet;
import com.sap.cloud.mobile.odata.Property;
import com.sap.appodatav4.service.OfflineWorkerUtil;

import java.util.WeakHashMap;

import static com.sap.appodatav4.mdui.EntitySetListActivity.EntitySetName.OrderSet;

/*
 * Repository factory to construct repository for an entity set
 */
public class RepositoryFactory {

    /*
     * Cache all repositories created to avoid reconstruction and keeping the entities of entity set
     * maintained by each repository in memory. Use a weak hash map to allow recovery in low memory
     * conditions
     */
    private WeakHashMap<String, Repository> repositories;
    /**
     * Construct a RepositoryFactory instance. There should only be one repository factory and used
     * throughout the life of the application to avoid caching entities multiple times.
     */
    public RepositoryFactory() {
        repositories = new WeakHashMap<>();
    }

    /**
     * Construct or return an existing repository for the specified entity set
     * @param entitySet - entity set for which the repository is to be returned
     * @param orderByProperty - if specified, collection will be sorted ascending with this property
     * @return a repository for the entity set
     */
    public Repository getRepository(@NonNull EntitySet entitySet, @Nullable Property orderByProperty) {
        Container container = OfflineWorkerUtil.getContainer();
        String key = entitySet.getLocalName();
        Repository repository = repositories.get(key);
        if (repository == null) {
            if (key.equals(EntitySets.orderItemSet.getLocalName())) {
                repository = new Repository<OrderItem>(container, EntitySets.orderItemSet, orderByProperty);
            } else if (key.equals(EntitySets.orderSet.getLocalName())) {
                repository = new Repository<Order>(container, EntitySets.orderSet, orderByProperty);
            } else {
                throw new AssertionError("Fatal error, entity set[" + key + "] missing in generated code");
            }
            repositories.put(key, repository);


        }
        return repository;
    }

    /**
     * Get rid of all cached repositories
     */
    public void reset() {
        repositories.clear();
    }
 }
