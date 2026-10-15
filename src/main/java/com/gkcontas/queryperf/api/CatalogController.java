package com.gkcontas.queryperf.api;

import com.gkcontas.queryperf.catalog.Author;
import com.gkcontas.queryperf.catalog.Book;
import com.gkcontas.queryperf.catalog.Review;
import com.gkcontas.queryperf.catalog.repository.BookRepository;
import com.gkcontas.queryperf.projection.BookSummaryDto;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * The catalogue itself, served the way the scenarios concluded it should be.
 *
 * <p>Three endpoints, three of the repairs in production use rather than under
 * measurement: the listing is the two-statement page, the summaries are the constructor
 * expression, and the detail fetches the two collections in one statement each instead
 * of in one cartesian statement.
 */
@RestController
@RequestMapping("/api/books")
public class CatalogController {

    private static final int MAX_PAGE_SIZE = 200;

    private final BookRepository books;

    public CatalogController(BookRepository books) {
        this.books = books;
    }

    /** The page of ids, then those ids with their reviews. Two statements, always. */
    @GetMapping
    @Transactional(readOnly = true)
    public List<BookView> list(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page must not be negative and size must be between 1 and " + MAX_PAGE_SIZE);
        }
        List<Long> ids = books.findIdPage(PageRequest.of(page, size));
        if (ids.isEmpty()) {
            return List.of();
        }
        return books.findWithReviewsByIds(ids).stream()
                .map(book -> new BookView(book.getId(), book.getTitle(), book.getIsbn(),
                        book.getPublishedYear(), book.getPrice(), book.getFormat().name(),
                        book.getReviews().size()))
                .toList();
    }

    /** Three columns, named in the query. No entity is materialised. */
    @GetMapping("/summaries")
    @Transactional(readOnly = true)
    public List<BookSummaryDto> summaries() {
        return books.findAllSummaryDto();
    }

    /** One statement per collection, which is the repair for the cartesian product. */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public BookView.Detail detail(@PathVariable long id) {
        Book withAuthors = books.findWithAuthors(id).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "no book with id " + id));
        Book withReviews = books.findWithReviews(id).stream().findFirst().orElseThrow();

        List<String> authors = withAuthors.getAuthors().stream().map(Author::getName).toList();
        List<Review> reviews = withReviews.getReviews();
        double average = reviews.stream().mapToInt(Review::getRating).average().orElse(0);

        return new BookView.Detail(withAuthors.getId(), withAuthors.getTitle(),
                withAuthors.getIsbn(), withAuthors.getPublishedYear(), withAuthors.getPrice(),
                withAuthors.getFormat().name(), withAuthors.getPublisher().getName(),
                authors, reviews.size(), Math.round(average * 100) / 100.0);
    }
}
