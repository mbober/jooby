/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Default registry which use a simply key/value mechanism for storing and retrieving services.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface ServiceRegistry extends Registry {
  /**
   * Registered service keys.
   *
   * @return Service keys.
   */
  @Nonnull Set<ServiceKey<?>> keySet();

  /**
   * Retrieve a service/resource by key.
   *
   * @param key Service/resource key.
   * @param <T> Service/resource type.
   * @return Service.
   * @throws RegistryException If there was a runtime failure while providing an instance.
   */
  default @Nonnull <T> T get(@Nonnull ServiceKey<T> key) {
    T service = getOrNull(key);
    if (service == null) {
      throw new RegistryException("Service not found: " + key);
    }
    return service;
  }

  /**
   * Retrieve a service/resource by key.
   *
   * @param type Service/resource key.
   * @param <T> Service/resource type.
   * @return Service.
   * @throws RegistryException If there was a runtime failure while providing an instance.
   */
  default @Nonnull <T> T get(@Nonnull Class<T> type) {
    return get(ServiceKey.key(type));
  }

  /**
   * Retrieve an existing service or <code>null</code> if not exists.
   *
   * @param key Service/resource key.
   * @param <T> Service/resource type.
   * @return Service or <code>null</code>.
   */
  @Nullable <T> T getOrNull(@Nonnull ServiceKey<T> key);

  /**
   * Retrieve an existing service or <code>null</code> if not exists.
   *
   * @param type Service/resource key.
   * @param <T> Service/resource type.
   * @return Service or <code>null</code>.
   */
  default @Nullable <T> T getOrNull(@Nonnull Class<T> type) {
    return getOrNull(ServiceKey.key(type));
  }

  /**
   * Put a service in this registry.  This method overrides any previous registered service.
   *
   * @param key Service/resource key.
   * @param service Service instance.
   * @param <T> Service type.
   * @return Previously registered service or <code>null</code>.
   */
  @Nullable <T> T put(@Nonnull ServiceKey<T> key, T service);

  /**
   * Put a service in this registry. This method overrides any previous registered service.
   *
   * @param type Service/resource key.
   * @param service Service instance.
   * @param <T> Service type.
   * @return Previously registered service or <code>null</code>.
   */
  default @Nullable <T> T put(@Nonnull Class<T> type, T service) {
    return put(ServiceKey.key(type), service);
  }

  /**
   * Put/register a service in this registry if there isn't the same service already registered.
   *
   *
   * @param key Service/resource key.
   * @param service Service instance.
   * @param <T> Service type.
   * @return Previously registered service or <code>null</code>.
   */
  @Nullable <T> T putIfAbsent(@Nonnull ServiceKey<T> key, T service);

  /**
   * Put/register a service in this registry if there isn't the same service already registered.
   *
   *
   * @param type Service/resource key.
   * @param service Service instance.
   * @param <T> Service type.
   * @return Previously registered service or <code>null</code>.
   */
  default @Nullable <T> T putIfAbsent(@Nonnull Class<T> type, T service) {
    return putIfAbsent(ServiceKey.key(type), service);
  }

  default @Nonnull @Override <T> T require(@Nonnull Class<T> type) {
    return get(ServiceKey.key(type));
  }

  default @Nonnull @Override <T> T require(@Nonnull Class<T> type, @Nonnull String name) {
    return get(ServiceKey.key(type, name));
  }
}
