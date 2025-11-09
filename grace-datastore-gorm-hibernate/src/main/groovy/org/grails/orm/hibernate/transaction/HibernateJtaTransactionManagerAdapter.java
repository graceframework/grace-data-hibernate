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
package org.grails.orm.hibernate.transaction;

import javax.transaction.xa.XAResource;

import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Adapter for adding transaction controlling hooks for supporting
 * Hibernate's org.hibernate.engine.transaction.Isolater class's interaction with transactions
 * <p>
 * This is required when there is no real JTA transaction manager in use and Spring's
 * {@link TransactionAwareDataSourceProxy} is used.
 * <p>
 * Without this solution, using Hibernate's TableGenerator identity strategies will fail to support transactions.
 * The id generator will commit the current transaction and break transactional behaviour.
 * <p>
 * The javadoc of Hibernate's {@code TableHiLoGenerator} states this. However this isn't mentioned in the javadocs of other TableGenerators.
 *
 * @author Lari Hotari
 */
public class HibernateJtaTransactionManagerAdapter implements TransactionManager {

    PlatformTransactionManager springTransactionManager;

    ThreadLocal<TransactionStatus> currentTransactionHolder = new ThreadLocal<>();

    public HibernateJtaTransactionManagerAdapter(PlatformTransactionManager springTransactionManager) {
        this.springTransactionManager = springTransactionManager;
    }

    @Override
    public void begin() {
        TransactionDefinition definition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.currentTransactionHolder.set(this.springTransactionManager.getTransaction(definition));
    }

    @Override
    public void commit() throws
            SecurityException, IllegalStateException {
        this.springTransactionManager.commit(getAndRemoveStatus());
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException {
        this.springTransactionManager.rollback(getAndRemoveStatus());
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException {
        this.currentTransactionHolder.get().setRollbackOnly();
    }

    protected TransactionStatus getAndRemoveStatus() {
        TransactionStatus status = this.currentTransactionHolder.get();
        this.currentTransactionHolder.remove();
        return status;
    }

    @Override
    public int getStatus() {
        TransactionStatus status = this.currentTransactionHolder.get();
        return convertToJtaStatus(status);
    }

    protected static int convertToJtaStatus(TransactionStatus status) {
        if (status != null) {
            if (status.isCompleted()) {
                return Status.STATUS_UNKNOWN;
            }
            else if (status.isRollbackOnly()) {
                return Status.STATUS_MARKED_ROLLBACK;
            }
            else {
                return Status.STATUS_ACTIVE;
            }
        }
        else {
            return Status.STATUS_NO_TRANSACTION;
        }
    }

    @Override
    public Transaction getTransaction() {
        return new TransactionAdapter(this.springTransactionManager, this.currentTransactionHolder);
    }

    @Override
    public void resume(Transaction tobj) throws IllegalStateException {
        TransactionAdapter transaction = (TransactionAdapter) tobj;
        // commit the PROPAGATION_NOT_SUPPORTED transaction returned in suspend
        this.springTransactionManager.commit(transaction.transactionStatus);
    }

    @Override
    public Transaction suspend() {
        this.currentTransactionHolder.set(this.springTransactionManager.getTransaction(
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NOT_SUPPORTED)));
        return new TransactionAdapter(this.springTransactionManager, this.currentTransactionHolder);
    }

    @Override
    public void setTransactionTimeout(int seconds) {

    }

    private static class TransactionAdapter implements Transaction {

        PlatformTransactionManager springTransactionManager;

        TransactionStatus transactionStatus;

        ThreadLocal<TransactionStatus> currentTransactionHolder;

        TransactionAdapter(PlatformTransactionManager springTransactionManager, ThreadLocal<TransactionStatus> currentTransactionHolder) {
            this.springTransactionManager = springTransactionManager;
            this.currentTransactionHolder = currentTransactionHolder;
            this.transactionStatus = currentTransactionHolder.get();
        }

        @Override
        public void commit() throws
                SecurityException, IllegalStateException {
            this.springTransactionManager.commit(this.transactionStatus);
            this.currentTransactionHolder.remove();
        }

        @Override
        public boolean delistResource(XAResource xaRes, int flag) throws IllegalStateException, SystemException {
            return false;
        }

        @Override
        public boolean enlistResource(XAResource xaRes) throws RollbackException, IllegalStateException,
                SystemException {
            return false;
        }

        @Override
        public int getStatus() throws SystemException {
            return convertToJtaStatus(this.transactionStatus);
        }

        @Override
        public void registerSynchronization(final Synchronization sync) throws RollbackException, IllegalStateException,
                SystemException {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCompletion() {
                    sync.beforeCompletion();
                }

                @Override
                public void afterCompletion(int status) {
                    int jtaStatus;
                    if (status == TransactionSynchronization.STATUS_COMMITTED) {
                        jtaStatus = Status.STATUS_COMMITTED;
                    }
                    else if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                        jtaStatus = Status.STATUS_ROLLEDBACK;
                    }
                    else {
                        jtaStatus = Status.STATUS_UNKNOWN;
                    }
                    sync.afterCompletion(jtaStatus);
                }

                public void suspend() {
                }

                public void resume() {
                }

                public void flush() {
                }

                public void beforeCommit(boolean readOnly) {
                }

                public void afterCommit() {
                }
            });
        }

        @Override
        public void rollback() throws IllegalStateException, SystemException {
            this.springTransactionManager.rollback(this.transactionStatus);
            this.currentTransactionHolder.remove();
        }

        @Override
        public void setRollbackOnly() throws IllegalStateException, SystemException {
            this.transactionStatus.setRollbackOnly();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            else if (obj == null) {
                return false;
            }
            else if (obj.getClass() == TransactionAdapter.class) {
                TransactionAdapter other = (TransactionAdapter) obj;
                if (other.transactionStatus == this.transactionStatus) {
                    return true;
                }
                else if (other.transactionStatus != null) {
                    return other.transactionStatus.equals(this.transactionStatus);
                }
                else {
                    return false;
                }
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return this.transactionStatus != null ? this.transactionStatus.hashCode() : System.identityHashCode(this);
        }

    }

}
