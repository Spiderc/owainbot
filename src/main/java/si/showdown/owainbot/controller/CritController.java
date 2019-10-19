package si.showdown.owainbot.controller;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import si.showdown.owainbot.HibernateUtil;
import si.showdown.owainbot.bean.Crit;

public class CritController {

	@SuppressWarnings("unchecked")
	public Crit getRandomQuote(String param) {
		boolean isCharacter = false;
		boolean isGame = false;
		
		if(!param.equals("")) {
			List<String> games = getGames();
			if(games.contains(param.toLowerCase())) {
				isGame = true;
			} else {
				isCharacter = true;
			}
		}
		
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();

		Criteria criteria = session.createCriteria(Crit.class);
		if(isCharacter) {
			criteria.add(Restrictions.eq("character", param).ignoreCase());
		} else if(isGame) {
			criteria.add(Restrictions.eq("game", param).ignoreCase());
		}
		criteria.add(Restrictions.sqlRestriction("1=1 order by rand()"));
		criteria.setMaxResults(1);

		session.getTransaction().commit();
		List<Crit> quote = criteria.list();

		session.close();

		return quote.get(0);
	}

	@SuppressWarnings("unchecked")
	public List<String> getGames() {
		List<String> games = new ArrayList<>();
		
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();

		Criteria criteria = session.createCriteria(Crit.class);
		criteria.setProjection(Projections.distinct(Projections.property("game")));

		session.getTransaction().commit();
		List<String> rawGames = criteria.list();

		session.close();
		
		for(String game:rawGames) {
			games.add(game.toLowerCase());
		}

		return games;
	}
}
