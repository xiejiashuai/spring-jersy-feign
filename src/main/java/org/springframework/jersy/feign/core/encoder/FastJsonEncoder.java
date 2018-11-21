package org.springframework.jersy.feign.core.encoder;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig;
import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;

import java.lang.reflect.Type;

/**
 * Implement {@link Encoder}
 *
 * @author jiashuai.xie
 */
public class FastJsonEncoder implements Encoder {

    private SerializeConfig config = null;

    public FastJsonEncoder() {
        this(null);
    }

    public FastJsonEncoder(SerializeConfig config) {
        if (null != config) {
            this.config = config;
        } else {
            this.config = SerializeConfig.getGlobalInstance();
        }
    }


    @Override
    public void encode(Object obj, Type type, RequestTemplate template) throws EncodeException {
        template.body(JSON.toJSONString(obj,config));
    }

}
