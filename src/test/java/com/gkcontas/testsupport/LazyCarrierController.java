package com.gkcontas.testsupport;

import com.gkcontas.queryperf.catalog.Publisher;
import com.gkcontas.queryperf.catalog.repository.BookRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * A controller that exists only in the tests, and only in order to be wrong.
 *
 * <p>It hands Jackson an uninitialised proxy: the book is read outside any transaction
 * of the caller's, {@code getPublisher()} returns the proxy without touching it, and
 * whether the response succeeds is decided entirely by whether the session is still
 * open when the serialiser walks the object - which is what {@code open-in-view}
 * decides.
 *
 * <p>Not in the application, because an endpoint that serialises entities is the shape
 * this project argues against. And not in package {@code com.gkcontas.queryperf}
 * either: the application's component scan covers that package and the test classpath
 * sits in it, so a {@code @RestController} declared there would be registered in every
 * context in the project - twice over, once by the scan and once by the
 * {@code @TestConfiguration} - and every test would fail on an ambiguous mapping.
 */
@TestConfiguration
public class LazyCarrierController {

    // No @Bean method for the endpoint: importing a configuration also registers its
    // nested components, so declaring it both ways maps the route twice and the context
    // fails to start.

    /**
     * Holds the proxy without exposing it. Jackson is never handed the proxy itself -
     * it would fail on the proxy's own interceptor property rather than on the lazy
     * load - it is handed a getter that reaches through it, which is exactly what a
     * serialiser walking an entity graph does.
     */
    public static final class LazyCarrier {

        private final String title;
        private final Publisher publisher;

        LazyCarrier(String title, Publisher publisher) {
            this.title = title;
            this.publisher = publisher;
        }

        public String getTitle() {
            return title;
        }

        /** Initialises the proxy at serialisation time, or throws trying. */
        public String getPublisherName() {
            return publisher.getName();
        }
    }

    @RestController
    public static class LazyCarrierEndpoint {

        private final BookRepository books;

        LazyCarrierEndpoint(BookRepository books) {
            this.books = books;
        }

        @GetMapping("/test-only/lazy/{id}")
        public LazyCarrier lazyCarrier(@PathVariable long id) {
            var book = books.findById(id).orElseThrow();
            return new LazyCarrier(book.getTitle(), book.getPublisher());
        }
    }
}
