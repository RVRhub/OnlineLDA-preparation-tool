package infrascructure.data.email;

import infrascructure.data.Config;
import infrascructure.data.Resource;
import infrascructure.data.list.BigList;
import infrascructure.data.readers.CacheableReader;
import infrascructure.data.readers.SimpleCachedList;
import infrascructure.data.serialize.RawResourceSerializer;
import infrascructure.data.serialize.RawSerializersFactory;
import infrascructure.data.util.Trace;
import org.springframework.beans.factory.annotation.Autowired;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: shredinger
 * Date: 2/8/14
 * Time: 1:02 PM
 * Project: IntelligentSearch
 */
public class RawEmailsRepository extends CacheableReader<Resource> {
    private final int MAX_CACHE_SIZE = 20;

    protected final Config config;
    private final int max_docs_count;
    private final RawSerializersFactory serializersFactory;
    private final EmailReader emailReader;

    @Autowired
    private EmailFilter filter;

    protected BigList<Resource> rawdocs;

    public RawEmailsRepository(Config config, RawSerializersFactory serializersFactory, EmailReader emailReader) {
        this.config = config;
        this.serializersFactory = serializersFactory;
        this.emailReader = emailReader;

        max_docs_count = config.getPropertyInt(Config.MAX_DOCS_COUNT);
        String sourceDir = config.getProperty(Config.RAW_EMAILS_REPOSITORY);
        RawResourceSerializer serializer = this.serializersFactory.createSimpleSerializer(sourceDir);
        rawdocs = new SimpleCachedList<Resource>(sourceDir, MAX_CACHE_SIZE, serializer);
    }

    @Override
    public Resource get(Integer i) {
        return i >= max_docs_count ? null : rawdocs.get(i);
    }

    @Override
    public void readAll() throws IOException {
        Iterator<Message> messagesIterator = emailReader.getMessagesIterator();
        int id = 0;
        while (messagesIterator.hasNext()){
            Message message = messagesIterator.next();
            if(filter.accept(message)){
                try {
                    Trace.trace("Message " + id + " was read [" + truncate(message.getSubject(), 100) + "]");
                    String email = EmailUtil.getEmail(message);
                    Resource resource = new Resource(email, String.valueOf(id));
                    rawdocs.add(resource);
                    id ++;
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String truncate(String s, int length){
        return s.substring(0, Math.min(length, s.length()));
    }


}
