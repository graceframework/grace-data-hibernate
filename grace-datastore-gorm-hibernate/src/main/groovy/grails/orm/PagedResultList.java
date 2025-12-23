/*
 * Copyright 2016-2025 the original author or authors.
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
package grails.orm;

import org.grails.datastore.mapping.query.Query;

/**
 * A result list for Criteria list calls, which is aware of the totalCount for
 * the paged result.
 *
 * @author Siegfried Puchbauer
 * @since 1.0
 * @deprecated Use {@link org.grails.orm.hibernate.query.PagedResultList} instead.
 */
@SuppressWarnings({ "rawtypes" })
@Deprecated
public class PagedResultList extends grails.gorm.PagedResultList {

    public PagedResultList(Query query) {
        super(query);
    }

}
