package org.dspace.app.rest.model.hateoas;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.utils.Utils;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public abstract class DSpaceResource<T extends RestModel> extends ResourceSupport {
	@JsonUnwrapped
	private final T data;

	@JsonProperty(value="_embedded")
	@JsonInclude(Include.NON_EMPTY)
	private Map<String, Object> embedded = new HashMap<String, Object>();

	public DSpaceResource(T data, Utils utils) {
		this.data = data;

		try {
			for (PropertyDescriptor pd : Introspector.getBeanInfo(data.getClass()).getPropertyDescriptors()) {
				Method readMethod = pd.getReadMethod();
				if (readMethod != null && !"class".equals(pd.getName())) {
					if (RestModel.class.isAssignableFrom(readMethod.getReturnType())) {
						this.add(utils.linkToSubResource(data, pd.getName()));
						RestModel linkedObject = (RestModel) readMethod.invoke(data);
						if (linkedObject != null) {
							embedded.put(pd.getName(),
									utils.getResourceRepository(linkedObject.getType()).wrapResource(linkedObject));
						} else {
							embedded.put(pd.getName(), null);
						}

						Method writeMethod = pd.getWriteMethod();
						writeMethod.invoke(data, new Object[] { null });
					}
				}
			}
		} catch (IntrospectionException | IllegalArgumentException | IllegalAccessException
				| InvocationTargetException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		this.add(utils.linkToSingleResource(data, Link.REL_SELF));
	}

	public Map<String, Object> getEmbedded() {
		return embedded;
	}

	public T getData() {
		return data;
	}
}
