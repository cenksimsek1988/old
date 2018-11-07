package com.softactive;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.softactive.core.manager.CoreHttpRequester;
import com.softactive.core.object.CoreConstants;
import com.softactive.core.object.MyError;
import com.softactive.grwa.fred.manager.FredPriceHandler;
import com.softactive.grwa.fred.manager.FredPriceRequestHelper;
import com.softactive.grwa.object.GrwaConstants;
import com.softactive.grwa.object.Indicator;
import com.softactive.grwa.object.Price;
import com.softactive.grwa.object.Region;
import com.softactive.grwa.object.RiskFactor;
import com.softactive.grwa.object.UpdateError;
import com.softactive.grwa.service.RiskFactorHelper;
import com.softactive.grwa.wb.manager.WorldBankPriceHandler;
import com.softactive.grwa.wb.manager.WorldBankPriceRequestHelper;
import com.softactive.service.IndicatorRepository;
import com.softactive.service.PriceRepository;
import com.softactive.service.RegionRepository;
import com.softactive.service.RiskFactorRepository;
import com.softactive.service.UpdateErrorRepository;

@Component
public class Example implements GrwaConstants, CoreConstants{

	private RiskFactorHelper rfsCenk;
	@Autowired
	private RiskFactorRepository rfsFatih;
	@Autowired
	private RegionRepository rsFatih;
	@Autowired
	private IndicatorRepository isFatih;
	@Autowired
	private PriceRepository psFatih;
	@Autowired
	private UpdateErrorRepository uesFatih;

	// http requestleri yapacak bir obje lazım
	// sen kendi istediğin gibi oluşturursun, bu benim classımın nesnesi
	private CoreHttpRequester<WorldBankPriceRequestHelper> wbHttpRequester = new CoreHttpRequester<WorldBankPriceRequestHelper>() {
		@Override
		public void onAnswer(WorldBankPriceRequestHelper rHelper, boolean succcessfull, String answer) {
			if(succcessfull) {
				// handle edicez, bitince aşağıda yazılı recursive döngü ile 
				// hepsinin up-to-date olduğunu görene kadar
				// sıradaki risk factor işleme alınacak
				// aynı risk faktör için ayrı http sorgusu atılması gereken ek sayfalar olabilir
				// bu yüzden handle ederken hem helper'ımı (yeni urli generate etsin)
				// hem de string formatına çevirdiğim webden gelen cevabı parametre alıyor 
				handleWBResponse(rHelper, answer);
			} else {
				// benim http request helper classım http request fail olunca successfull parametresini
				// false döndürüyor
				// kendi iş mantığımda bu sadece bağlantı hataları olduğunda mümkün
				// eğer veri kaynağımızın apileri değişirse de failure alabiliriz
				// bu senaryoyu da istersen sen değerlendirirsin

				// eğer dokunmazsak aşağıda yazdığım recursive döngü gereği veri tabanına tekrar risk faktör sorgusu yapılacak
				// mevcut http requestte en son kullandığımız risk faktör hata alındığı için güncellenmemiş görünecek
				// ve  veri tabanı güncellenmemiş risk faktör sorgusunda yine aynı risk faktör geri dönecek
				// bu masraftan kısmak adına http requesti ben doğrudan tekrar ettiriyorum
				// ta ki internet bağlantı hatası çözülsün
				// parametre url yerine kendi clasımın objesini kullanıyorum, çok sayfalı api cevaplarını yönetmekte
				// yardımcı oluyor
				//TODO
				// burada sen kendi http requestini tekrar edeceksin, belki timer koyarsın
				wbHttpRequester.request(rHelper);
			}
		}
	};

	private CoreHttpRequester<FredPriceRequestHelper> frHttpRequester = new CoreHttpRequester<FredPriceRequestHelper>() {
		@Override
		public void onAnswer(FredPriceRequestHelper rHelper, boolean succcessfull, String answer) {
			if(succcessfull) {
				// handle edicez, bitince aşağıda yazılı recursive döngü ile 
				// hepsinin up-to-date olduğunu görene kadar
				// sıradaki risk factor işleme alınacak
				// aynı risk faktör için ayrı http sorgusu atılması gereken ek sayfalar olabilir
				// bu yüzden handle ederken hem helper'ımı (yeni urli generate etsin)
				// hem de string formatına çevirdiğim webden gelen cevabı parametre alıyor 
				handleFRResponse(rHelper, answer);
			} else {
				// benim http request helper classım http request fail olunca successfull parametresini
				// false döndürüyor
				// kendi iş mantığımda bu sadece bağlantı hataları olduğunda mümkün
				// eğer veri kaynağımızın apileri değişirse de failure alabiliriz
				// bu senaryoyu da istersen sen değerlendirirsin

				// eğer dokunmazsak aşağıda yazdığım recursive döngü gereği veri tabanına tekrar risk faktör sorgusu yapılacak
				// mevcut http requestte en son kullandığımız risk faktör hata alındığı için güncellenmemiş görünecek
				// ve  veri tabanı güncellenmemiş risk faktör sorgusunda yine aynı risk faktör geri dönecek
				// bu masraftan kısmak adına http requesti ben doğrudan tekrar ettiriyorum
				// ta ki internet bağlantı hatası çözülsün
				// parametre url yerine kendi clasımın objesini kullanıyorum, çok sayfalı api cevaplarını yönetmekte
				// yardımcı oluyor
				//TODO
				// burada sen kendi http requestini tekrar edeceksin, belki timer koyarsın
				frHttpRequester.request(rHelper);
			}
		}
	};

	public Example() {
		// benim service'im aslında bi helper, static de olabilirdi
		rfsCenk = new RiskFactorHelper();

		// senin servislerini sen oluşturacaksın ya da autowire edeceksin
		// TODO
		rfsFatih = rfsFatih;
		rsFatih = rsFatih;
		isFatih = isFatih;
		psFatih = psFatih;
		uesFatih = uesFatih;
	}

	public void update() {
		System.out.println("starting to update");
		//		updateWorldBank();
		updateFred();
	}

	private void updateWorldBank() {
		updateNextWB();
	}
	private void updateFred() {
		updateNextFR();
	}

	private int frFrqIndex = 0;
	private void updateNextFR() {
		// Güncel datası eksik Risk Faktor sorgusu için gereken sql
		// Bu sql hesaplanırken şu anki tarih ve frekans bilgisi ile
		// güncel veri eksikliğini kontrol edecek where koşulu oluşturuluyor.
		// veri kaynağı tipine göre de sorguluyorum çünkü farklı kaynakların
		// hem url hesaplamaları hem de
		// döndürdükleri cevaptan obje maplemesi farklı

		// benim servisim sql'i oluşturuyor
		String sql = rfsCenk.pickSql(SOURCE_FRED, FREQUENCIES[frFrqIndex]);

		// senin servisin sql'i çalıştırıp senin Risk Factor objelerinden bir liste döndürüyor
		List<RiskFactor> riskFactors = rfsFatih.query(sql);

		if(riskFactors.isEmpty()) {
			if(frFrqIndex<FREQUENCIES.length-1) {
				frFrqIndex++;
				updateNextFR();
			} else {
				return;
			}
		} else {
			// limit 1 le sorgu attığımızdan dönerse tek eleman döner
			RiskFactor rfFatih = riskFactors.get(0);

			// request edilecek url'in hesaplanması için, Risk Factor objesi yetmiyor
			// ilgili Region ve Indicator objeleri de gerekli
			// TODO
			Region rFatih = rsFatih.find(rfFatih.getRegionId()); // örnek, sen kendi region objeni muhtemelen başka türlü çağıracaksın
			Indicator iFatih = isFatih.findIndicatorById(rfFatih.getIndicatorCode()); // indicator için de aynı şekilde

			// senin objelerinin bir yerde benim objelerime dönüşmesi gerek
			// TODO
			RiskFactor rfCenk = rfFatih; // burada dönüştürmeyi yaptığımı farzediyorum
			Region rCenk = rFatih; // belki burada senin bir converter class'ın yardımcı olacak
			Indicator iCenk = iFatih;

			// FRED request helper objesi oluşturuyoruz, yukarıdaki üçlü, constructor'ın parametreleri
			FredPriceRequestHelper rHelper = new FredPriceRequestHelper(rfCenk, rCenk, iCenk);

			// parametreleri hesaplanmış, eklenmiş ve String formatına dönüştürülmüş
			// sorguya hazır url artık oluşturulabilir
			// aşağıdaki methodla çağırabiliyorsun
			String url = rHelper.calculatedUrl();

			// Bu url ile http request atıyorsun ve cevabın body kısmını String'e çeviriyorsun
			//TODO
			frHttpRequester.request(rHelper);
			// benim http request helper'ım Async çalıştığından webden cevap alındıktan sonra dönecek cevap
			// handleWBResponse(String response) methodunda işlenecek
			// yine yukarıda da belirttiğim gibi, url ile değil kendi custom objemle http request yapmayı tercih ediyorum
		}
	}

	private int wbFrqIndex = 0;
	private void updateNextWB() {
		// Güncel datası eksik Risk Faktor sorgusu için gereken sql
		// Bu sql hesaplanırken şu anki tarih ve frekans bilgisi ile
		// güncel veri eksikliğini kontrol edecek where koşulu oluşturuluyor.
		// veri kaynağı tipine göre de sorguluyorum çünkü farklı kaynakların
		// hem url hesaplamaları hem de
		// döndürdükleri cevaptan obje maplemesi farklı

		// benim servisim sql'i oluşturuyor
		String sql = rfsCenk.pickSql(SOURCE_WORLD_BANK, FREQUENCIES[wbFrqIndex]);

		// senin servisin sql'i çalıştırıp senin Risk Factor objelerinden bir liste döndürüyor
		List<RiskFactor> riskFactors = rfsFatih.query(sql);

		if(riskFactors.isEmpty()) {
			if(wbFrqIndex<FREQUENCIES.length-1) {
				wbFrqIndex++;
				updateNextWB();
			} else {
				return;
			}
		} else {
			// limit 1 le sorgu attığımızdan dönerse tek eleman döner
			RiskFactor rfFatih = riskFactors.get(0);

			// request edilecek url'in hesaplanması için, Risk Factor objesi yetmiyor
			// ilgili Region ve Indicator objeleri de gerekli
			// TODO
			Region rFatih = rsFatih.find(rfFatih.getRegionId()); // örnek, sen kendi region objeni muhtemelen başka türlü çağıracaksın
			Indicator iFatih = isFatih.findIndicatorById(rfFatih.getIndicatorCode()); // indicator için de aynı şekilde

			// senin objelerinin bir yerde benim objelerime dönüşmesi gerek
			// TODO
			RiskFactor rfCenk = rfFatih; // burada dönüştürmeyi yaptığımı farzediyorum
			Region rCenk = rFatih; // belki burada senin bir converter class'ın yardımcı olacak
			Indicator iCenk = iFatih;

			// World Bank request helper objesi oluşturuyoruz, yukarıdaki üçlü, constructor'ın parametreleri
			WorldBankPriceRequestHelper rHelper = new WorldBankPriceRequestHelper(rfCenk, rCenk, iCenk);

			// parametreleri hesaplanmış, eklenmiş ve String formatına dönüştürülmüş
			// sorguya hazır url artık oluşturulabilir
			// aşağıdaki methodla çağırabiliyorsun
			String url = rHelper.calculatedUrl();

			// Bu url ile http request atıyorsun ve cevabın body kısmını String'e çeviriyorsun
			//TODO
			wbHttpRequester.request(rHelper);
			// benim http request helper'ım Async çalıştığından webden cevap alındıktan sonra dönecek cevap
			// handleWBResponse(String response) methodunda işlenecek
			// yine yukarıda da belirttiğim gibi, url ile değil kendi custom objemle http request yapmayı tercih ediyorum
		}
	}

	private void handleFRResponse(FredPriceRequestHelper rHelper, String answer) {
		// Handler'ı oluştururken sharedParams adında bir de Map'ten yararlanıyorum. Bu Map'in içinde
		// Dünya bankası handler'ı için, örneğin bir Risk Faktör objesi olmalı ki parse edeceğim
		// cevabın neyin cevabı olduğu, cevap ne olursa olsun - boş bile olsa - takip edebileyim.
		// bu Map'i de construct ettiğimiz request helper objesinden hazır halde alabiliriz.
		Map<String, Object> sharedParams = rHelper.getSharedParams();
		FredPriceHandler handler = new FredPriceHandler(sharedParams);

		// Artık cevabı parse edebilirim. Main thread'te senkronize çalışmasını istediğinden
		// seni error ve failure anlarında yapılması gerekenleri baştan tanımlamaya (Override)
		// mecbur bırakmayacağım. Onun yerine handle'ın sonunda bir sharedParams Map'ini
		// aşağıda açıkladığım key'lerle döndürüyorum

		// burada handle edilmiş cevap tutulacak
		// handle edilmiş ve maplenmiş nihai cevap
		// bazı temel key'ler ve dönen valule'lar:
		// PARAM_SUCCESS		=> dolu bir price listesi parse edebildim mi, boolean
		// null dönmez, false dönerse PARAM_ERROR null dönmez, PARAM_HAS_NEXT null döner
		// true dönerse PARAM_HAS_NEXT null dönmez ve PARAM_LIST null dönmediği gibi muhakkak doludur da
		// PARAM_HAS_NEXT		=> ek sayfa var mı, value kayıt booleandır
		// PARAM_ERROR			=> hata oluştuysa MyError objesi, oluşmadıysa null döner
		// PARAM_LIST			=> hata oluşmadıysa muhakkak dolu bir liste döner (örneğin Price listesi)
		// PARAM_RISK_FACTOR	=> güncellenmiş Risk Faktör objesi döner. Bunu risk_factor tablomuza geri yazmalıyız
		// null dönmez
		// RiskFactor'de region_id ve indicator_id'lerin yanı sıra frekans,
		// en son başarılı güncelleme tarihi
		// (günlük güncelleme sürecinde hangi risk faktörlerin güncel olmadığını
		// buradan takip ediyorum)
		// verinin başlangıç ve bitiş tarihlerini
		// (sık lazım olan bir bilgi olabileceğinden, price tablosuna sorgu ile
		// hesaplamak istemedim. Analizci indikatörün isminin yanında hangi dönemi
		// kapsadığını baştan görmek, ona göre indikatörü seçmek isteyebilir)
		// tutuyorum. güncel risk faktörün içinde de bu bilgiler mevcut
		// PARAM_REGION			=> request helper'ı oluştururken verdiğimiz region
		// null dönmez
		// PARAM_INDICATOR		=> request helper'ı oluştururken verdiğimiz indicator
		// null dönmez
		Map<String, Object> results = handler.handle(answer);

		// güncel risk faktör, cevap ister düzgün ister hatalı dönmüş olsun, bana lazım olacak
		// ilkin bu işi hallediyorum
		// map'ten refere ettiğim risk faktör objesi, aşağıda error kaydını tutarken de lazım olacak
		RiskFactor updatedRfCenk = (RiskFactor) results.get(PARAM_RISK_FACTOR);

		// Senin RiskFactor'une dönüştüreceksin
		// TODO
		RiskFactor updatedRfFatih = updatedRfCenk; // öylesi örnek

		// Veritabanına güncel riskfaktörü yazalım
		rfsFatih.save(updatedRfFatih);

		// başarısız cevap durumunda ek sayfa bilgisini maplemediğim için
		// bunu default false olarak başlatıyorum
		boolean hasNext = false;

		// herşey başarılı ve dolu bir price listesi maplenmiş mi? 
		boolean isSuccessfull = (boolean) results.get(PARAM_SUCCESS);

		if(isSuccessfull) {
			// başarılı
			// şimdi price'ları veri tabanına yazma işlerine başlayabiliriz

			// cevaplar, benim price formatımda burada bulunuyor
			List<Price> pricesCenk = (List<Price>) results.get(PARAM_LIST);

			// senin price listene çevireceksin
			// TODO
			List<Price> pricesFatih = pricesCenk; // burada dönüştürdüğünü varsayıyorum
			psFatih.save(pricesFatih); // Nihayet tek bir riskFactor için price listemiz kaydoldu!!
			System.out.println(pricesFatih.size() + " prices successfully saved");

			// ek sayfa varsa requestimizi ek sayfa parametresiyle yinelemek gerekecek
			hasNext = (boolean) results.get(PARAM_HAS_NEXT);
		} else {
			// başarısız bir cevap alınmış
			// o zaman hatayı daha sonra analiz etmek için veri tabanına yazıyoruz

			MyError er = (MyError) results.get(PARAM_ERROR);

			// Kaydetmek için bir UpdateError oluşturuyoruz. Bu benim değil
			// senin UpDateError class'ının bir objesi olmalı
			UpdateError ueFatih = new UpdateError();
			ueFatih.setRiskFactorId(updatedRfFatih.getId());

			// Region bilgisi, erroru kolay analiz edilecek halde kayıt altına almak için lazım
			Region rCenk = (Region) results.get(PARAM_REGION);
			ueFatih.setRegionName(rCenk.getName());

			// aynı durum indicatör için de geçerli
			Indicator iCenk = (Indicator) results.get(PARAM_INDICATOR);
			ueFatih.setIndicatorName(iCenk.getName());

			// hatamız FRED'e ait
			ueFatih.setSourceId(SOURCE_FRED);

			// hatanın tipi - bu sayede analizim kolaylaşacak
			ueFatih.setType(er.getType());

			// ve açıklayıcı mesaj
			ueFatih.setMessage(er.getResult());

			ueFatih.setFrequencyId(updatedRfFatih.getFrequencyCode());

			// kaydedebiliriz
			uesFatih.save(ueFatih);

			// bir de konsola özetle yazsın istersek
			handler.printError();
		}

		// bazen http requesten dönen cevap, ek sayfalar olduğunu söylüyor
		// öyleyse yeni bir risk faktöre http request yapmadan, ek sayfaları
		// tamamlamak gerek.
		if(hasNext) {
			// ek sayfa varsa bi dahaki http requesti için gereken url
			// sıradaki sayfa için hazırlansın
			rHelper.nextPage();
			// ve tekrar http request yapalım, recursive döngü oluştu
			// TODO
			// burada request işi sende, o döngüyü sen istediğin gibi tasarlayabilirsin
			frHttpRequester.request(rHelper);
		} else {
			// ek sayfa yoksa sıradaki sorguya geçelim
			updateNextFR();		
		}
	}

	private void handleWBResponse(WorldBankPriceRequestHelper rHelper, String answer) {
		// Handler'ı oluştururken sharedParams adında bir de Map'ten yararlanıyorum. Bu Map'in içinde
		// Dünya bankası handler'ı için, örneğin bir Risk Faktör objesi olmalı ki parse edeceğim
		// cevabın neyin cevabı olduğu, cevap ne olursa olsun - boş bile olsa - takip edebileyim.
		// bu Map'i de construct ettiğimiz request helper objesinden hazır halde alabiliriz.
		Map<String, Object> sharedParams = rHelper.getSharedParams();
		WorldBankPriceHandler handler = new WorldBankPriceHandler(sharedParams);

		// Artık cevabı parse edebilirim. Main thread'te senkronize çalışmasını istediğinden
		// seni error ve failure anlarında yapılması gerekenleri baştan tanımlamaya (Override)
		// mecbur bırakmayacağım. Onun yerine handle'ın sonunda bir sharedParams Map'ini
		// aşağıda açıkladığım key'lerle döndürüyorum

		// burada handle edilmiş cevap tutulacak
		// handle edilmiş ve maplenmiş nihai cevap
		// bazı temel key'ler ve dönen valule'lar:
		// PARAM_SUCCESS		=> dolu bir price listesi parse edebildim mi, boolean
		// null dönmez, false dönerse PARAM_ERROR null dönmez, PARAM_HAS_NEXT null döner
		// true dönerse PARAM_HAS_NEXT null dönmez ve PARAM_LIST null dönmediği gibi muhakkak doludur da
		// PARAM_HAS_NEXT		=> ek sayfa var mı, value kayıt booleandır
		// PARAM_ERROR			=> hata oluştuysa MyError objesi, oluşmadıysa null döner
		// PARAM_LIST			=> hata oluşmadıysa muhakkak dolu bir liste döner (örneğin Price listesi)
		// PARAM_RISK_FACTOR	=> güncellenmiş Risk Faktör objesi döner. Bunu risk_factor tablomuza geri yazmalıyız
		// null dönmez
		// RiskFactor'de region_id ve indicator_id'lerin yanı sıra frekans,
		// en son başarılı güncelleme tarihi
		// (günlük güncelleme sürecinde hangi risk faktörlerin güncel olmadığını
		// buradan takip ediyorum)
		// verinin başlangıç ve bitiş tarihlerini
		// (sık lazım olan bir bilgi olabileceğinden, price tablosuna sorgu ile
		// hesaplamak istemedim. Analizci indikatörün isminin yanında hangi dönemi
		// kapsadığını baştan görmek, ona göre indikatörü seçmek isteyebilir)
		// tutuyorum. güncel risk faktörün içinde de bu bilgiler mevcut
		// PARAM_REGION			=> request helper'ı oluştururken verdiğimiz region
		// null dönmez
		// PARAM_INDICATOR		=> request helper'ı oluştururken verdiğimiz indicator
		// null dönmez
		Map<String, Object> results = handler.handle(answer);

		// güncel risk faktör, cevap ister düzgün ister hatalı dönmüş olsun, bana lazım olacak
		// ilkin bu işi hallediyorum
		// map'ten refere ettiğim risk faktör objesi, aşağıda error kaydını tutarken de lazım olacak
		RiskFactor updatedRfCenk = (RiskFactor) results.get(PARAM_RISK_FACTOR);

		// Senin RiskFactor'une dönüştüreceksin
		// TODO
		RiskFactor updatedRfFatih = updatedRfCenk; // öylesi örnek

		// Veritabanına güncel riskfaktörü yazalım
		rfsFatih.save(updatedRfFatih);

		// başarısız cevap durumunda ek sayfa bilgisini maplemediğim için
		// bunu default false olarak başlatıyorum
		boolean hasNext = false;

		// herşey başarılı ve dolu bir price listesi maplenmiş mi? 
		boolean isSuccessfull = (boolean) results.get(PARAM_SUCCESS);

		if(isSuccessfull) {
			// başarılı
			// şimdi price'ları veri tabanına yazma işlerine başlayabiliriz

			// cevaplar, benim price formatımda burada bulunuyor
			List<Price> pricesCenk = (List<Price>) results.get(PARAM_LIST);

			// senin price listene çevireceksin
			// TODO
			List<Price> pricesFatih = pricesCenk; // burada dönüştürdüğünü varsayıyorum
			psFatih.save(pricesFatih); // Nihayet tek bir riskFactor için price listemiz kaydoldu!!
			System.out.println(pricesFatih.size() + " prices successfully saved");

			// ek sayfa varsa requestimizi ek sayfa parametresiyle yinelemek gerekecek
			hasNext = (boolean) results.get(PARAM_HAS_NEXT);
		} else {
			// başarısız bir cevap alınmış
			// o zaman hatayı daha sonra analiz etmek için veri tabanına yazıyoruz

			MyError er = (MyError) results.get(PARAM_ERROR);

			// Kaydetmek için bir UpdateError oluşturuyoruz. Bu benim değil
			// senin UpDateError class'ının bir objesi olmalı
			UpdateError ueFatih = new UpdateError();
			ueFatih.setRiskFactorId(updatedRfFatih.getId());

			// Region bilgisi, erroru kolay analiz edilecek halde kayıt altına almak için lazım
			Region rCenk = (Region) results.get(PARAM_REGION);
			ueFatih.setRegionName(rCenk.getName());

			// aynı durum indicatör için de geçerli
			Indicator iCenk = (Indicator) results.get(PARAM_INDICATOR);
			ueFatih.setIndicatorName(iCenk.getName());

			// hatamız dünya bankasına ait
			ueFatih.setSourceId(SOURCE_WORLD_BANK);

			// hatanın tipi - bu sayede analizim kolaylaşacak
			ueFatih.setType(er.getType());

			// ve açıklayıcı mesaj
			ueFatih.setMessage(er.getResult());

			ueFatih.setFrequencyId(updatedRfFatih.getFrequencyCode());

			// kaydedebiliriz
			uesFatih.save(ueFatih);

			// bir de konsola özetle yazsın istersek
			handler.printError();
		}

		// bazen http requesten dönen cevap, ek sayfalar olduğunu söylüyor
		// öyleyse yeni bir risk faktöre http request yapmadan, ek sayfaları
		// tamamlamak gerek.
		if(hasNext) {
			// ek sayfa varsa bi dahaki http requesti için gereken url
			// sıradaki sayfa için hazırlansın
			rHelper.nextPage();
			// ve tekrar http request yapalım, recursive döngü oluştu
			// TODO
			// burada request işi sende, o döngüyü sen istediğin gibi tasarlayabilirsin
			wbHttpRequester.request(rHelper);
		} else {
			// ek sayfa yoksa sıradaki sorguya geçelim
			updateNextWB();		
		}
	}
}
