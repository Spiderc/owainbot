package si.showdown.owainbot.controller;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import si.showdown.owainbot.HibernateUtil;
import si.showdown.owainbot.bean.Crit;

public class CritController {

	@SuppressWarnings("unchecked")
	public Crit getRandomQuote() {
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();

		Criteria criteria = session.createCriteria(Crit.class);
		criteria.add(Restrictions.sqlRestriction("1=1 order by rand()"));
		criteria.setMaxResults(1);

		session.getTransaction().commit();
		List<Crit> quote = criteria.list();
		
		session.close();
		
		return quote.get(0);
	}
}
