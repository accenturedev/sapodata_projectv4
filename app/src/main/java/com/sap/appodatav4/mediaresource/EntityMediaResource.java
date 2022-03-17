package com.sap.appodatav4.mediaresource;

import androidx.annotation.NonNull;
import com.sap.cloud.mobile.odata.EntitySet;
import com.sap.cloud.mobile.odata.EntityValue;
import com.sap.cloud.mobile.odata.Property;
import com.sap.cloud.mobile.odata.StreamLink;

/*
 * Utility class to support the use of Glide to download media resources
 */
public class EntityMediaResource {

    /**
     * Determine if an entity set has media resource
     * @param entitySet
     * @return true if entity type is a Media Linked Entry (MLE) or it has stream properties
     */
    public static boolean hasMediaResources(@NonNull EntitySet entitySet) {
        if (entitySet.getEntityType().isMedia() || entitySet.getEntityType().getStreamProperties().length() > 0) {
            return true;
        }
        return false;
    }


    /**
     * Return download Url for one of the media resource associated with the entity parameter.
     * @param entityValue
     * @param rootUrl
     * @return If the entity type associated with the entity parameter is a Media Linked Entry,
     *         the MLE url will be returned. Otherwise, download url for one of the stream
     *         properties will be returned.
     */
    public static String getMediaResourceUrl(@NonNull EntityValue entityValue, @NonNull String rootUrl) {
        if (entityValue.getEntityType().isMedia()) {
            return mediaLinkedEntityUrl(entityValue, rootUrl);
        } else {
            if (entityValue.getEntityType().getStreamProperties().length() > 0) {
                return namedResourceUrl(entityValue, rootUrl);
            }
        }
        return null;
    }

    /**
     * Get the media linked entity url
     * @param entityValue - entity whose MLE url is to return
     * @param rootUrl - OData Service base url
     * @return the media linked entity url or null if one cannot be constructed from the entity
     */
    private static String mediaLinkedEntityUrl(@NonNull EntityValue entityValue, @NonNull String rootUrl) {
        String mediaLink = entityValue.getMediaStream().getReadLink();
        if (mediaLink != null) {
            return rootUrl + mediaLink;
        }
        return null;
    }

    /**
     * Get the named resource URL. If there are more than one named resources, only one will be returned
     * @param entityValue entity whose MLE URL is to return
     * @param rootUrl
     * @return The named resource URL
     */
    private static String namedResourceUrl(@NonNull EntityValue entityValue, @NonNull String rootUrl) {
        Property namedResourceProp = entityValue.getEntityType().getStreamProperties().first();
        StreamLink streamLink = namedResourceProp.getStreamLink(entityValue);
        String mediaLink = streamLink.getReadLink();
        if (mediaLink != null) {
            return rootUrl + mediaLink;
        } else {
            // This is to get around the problem that after we writeToParcel and read it back, we lost the url for stream link
            // To be removed when bug is fixed
            if (entityValue.getReadLink() != null) {
                mediaLink = entityValue.getReadLink() + '/' + namedResourceProp.getName();
                return rootUrl + mediaLink;
            }
        }
        return null;
    }
}