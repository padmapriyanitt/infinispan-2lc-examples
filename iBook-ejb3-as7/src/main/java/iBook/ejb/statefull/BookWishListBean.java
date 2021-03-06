package iBook.ejb.statefull;

import iBook.domain.*;
import iBook.ejb.stateless.WishBookBean;
import iBook.ejb.stateless.remote.UserPaymentRemote;
import iBook.ejb.stateless.remote.WishBookRemote;
import iBook.utils.Utils;
import org.jboss.annotation.ejb.cache.simple.CacheConfig;
import org.jboss.ejb3.annotation.Clustered;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.naming.NamingException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Wish list statefull bean, which processes user's book wish list.
 */
@Stateful
@Clustered
@CacheConfig(maxSize=5000, idleTimeoutSeconds = 60000,removalTimeoutSeconds=1200000)
@Remote(BookWishList.class)
public class BookWishListBean implements Serializable, BookWishList {
	private static final long serialVersionUID = 1486647855776730094L;

	//The wish list of the user.
	private List<WishBook> wishList;
    private User user;

    @EJB
    WishBookRemote wishBookBean;

    @EJB
    UserPaymentRemote remote;

    public void init(User user) {
        this.user = user;
        wishList = wishBookBean.getWishList();
    }

    @Override
	public boolean addToWishList(Book book) {
        boolean isBookAdded = false;
		if(!wishList.contains(book)) {
            WishBook wishBook = new WishBook();
            wishBook.setBook(book);
            wishBook.setUser(user);

			wishList.add(wishBook);
            isBookAdded = true;
        }

        return isBookAdded;
	}

    @Override
	public List<WishBook> getWishList() {
		return wishList;
	}

    @Override
	public void checkout() {
		System.out.println("In checkout");
		
		UserPayments userPayment = new UserPayments();
		userPayment.setUser(user);
		double sum = 0;
		UserPayments2Books tmp = null;
		for(WishBook wishBook : wishList) {
			tmp = new UserPayments2Books();

            Book book = wishBook.getBook();
			tmp.setBook(book);

            sum += book.getPrice();
			userPayment.getUserPayments2Books().add(tmp);
		}
		
        userPayment.setPaid(sum);

        remote.saveUserPayments(userPayment);
        wishBookBean.deleteWishList(user);
        wishList.clear();
    }

    @Override
    @Remove
    public void finishWishList() {
        for(WishBook book : wishList) {
            System.out.println(book.getUser() + " " + book.getUser().getId());
            wishBookBean.saveWishBook(book);
        }
    }
}
