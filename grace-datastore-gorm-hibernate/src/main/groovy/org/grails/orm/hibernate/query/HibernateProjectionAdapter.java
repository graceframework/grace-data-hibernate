/*
 * Copyright 2011-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.orm.hibernate.query;

import java.util.HashMap;
import java.util.Map;

import org.grails.datastore.mapping.query.Query;

/**
 * Adapts Grails datastore API to Hibernate projections.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 2.0
 */
public class HibernateProjectionAdapter {

    private Query.Projection projection;

    private static Map<Class<?>, ProjectionAdapter> adapterMap = new HashMap<>();

    static {
        adapterMap.put(Query.AvgProjection.class, gormProjection -> {
            Query.AvgProjection avg = (Query.AvgProjection) gormProjection;
            return org.hibernate.criterion.Projections.avg(avg.getPropertyName());
        });
        adapterMap.put(Query.IdProjection.class, gormProjection -> org.hibernate.criterion.Projections.id());
        adapterMap.put(Query.SumProjection.class, gormProjection -> {
            Query.SumProjection avg = (Query.SumProjection) gormProjection;
            return org.hibernate.criterion.Projections.sum(avg.getPropertyName());
        });
        adapterMap.put(Query.DistinctPropertyProjection.class, gormProjection -> {
            Query.DistinctPropertyProjection avg = (Query.DistinctPropertyProjection) gormProjection;
            return org.hibernate.criterion.Projections.distinct(org.hibernate.criterion.Projections.property(avg.getPropertyName()));
        });
        adapterMap.put(Query.PropertyProjection.class, gormProjection -> {
            Query.PropertyProjection avg = (Query.PropertyProjection) gormProjection;
            return org.hibernate.criterion.Projections.property(avg.getPropertyName());
        });
        adapterMap.put(Query.CountProjection.class, gormProjection -> org.hibernate.criterion.Projections.rowCount());
        adapterMap.put(Query.CountDistinctProjection.class, gormProjection -> {
            Query.CountDistinctProjection cd = (Query.CountDistinctProjection) gormProjection;
            return org.hibernate.criterion.Projections.countDistinct(cd.getPropertyName());
        });
        adapterMap.put(Query.GroupPropertyProjection.class, gormProjection -> {
            Query.GroupPropertyProjection cd = (Query.GroupPropertyProjection) gormProjection;
            return org.hibernate.criterion.Projections.groupProperty(cd.getPropertyName());
        });
        adapterMap.put(Query.MaxProjection.class, gormProjection -> {
            Query.MaxProjection cd = (Query.MaxProjection) gormProjection;
            return org.hibernate.criterion.Projections.max(cd.getPropertyName());
        });
        adapterMap.put(Query.MinProjection.class, gormProjection -> {
            Query.MinProjection cd = (Query.MinProjection) gormProjection;
            return org.hibernate.criterion.Projections.min(cd.getPropertyName());
        });
    }

    public HibernateProjectionAdapter(Query.Projection projection) {
        this.projection = projection;
    }

    public org.hibernate.criterion.Projection toHibernateProjection() {
        ProjectionAdapter projectionAdapter = adapterMap.get(this.projection.getClass());
        if (projectionAdapter == null) {
            throw new UnsupportedOperationException("Unsupported projection used: " + this.projection.getClass().getName());
        }
        return projectionAdapter.toHibernateProjection(this.projection);
    }

    private interface ProjectionAdapter {

        org.hibernate.criterion.Projection toHibernateProjection(Query.Projection gormProjection);

    }

}
