package com.logicgate.farm.service;

import com.logicgate.farm.domain.Animal;
import com.logicgate.farm.domain.Barn;
import com.logicgate.farm.repository.AnimalRepository;
import com.logicgate.farm.repository.BarnRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AnimalServiceImpl implements AnimalService {

  private final AnimalRepository animalRepository;

  private final BarnRepository barnRepository;

  @Autowired
  public AnimalServiceImpl(AnimalRepository animalRepository, BarnRepository barnRepository) {
    this.animalRepository = animalRepository;
    this.barnRepository = barnRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Animal> findAll() {
    return animalRepository.findAll();
  }

  @Override
  public void deleteAll() {
    animalRepository.deleteAll();
  }

//  method called by addToFarm below to evenly disperse all farm animals across all barns once a new barn is built
  private void distributeAnimals(int animalSize, List<List<Animal>> animalMap, Barn barn) {
    List<Animal> newBarn = new ArrayList<>();
    int animalsPerBarn = animalSize / (animalMap.size() + 1);
    animalMap.forEach(animalList -> animalList.subList(animalsPerBarn, 20).forEach(animal -> newBarn.add(animal)));
    newBarn.forEach(animal -> {
        animal.setBarn(barn);
        animalRepository.save(animal);
      }
    );
    return;
  }

  @Override
  public Animal addToFarm(Animal animal) {
    List<Animal> animals = animalRepository.findAll().stream().filter(a -> a.getFavoriteColor() == animal.getFavoriteColor()).collect(Collectors.toList());
    List<List<Animal>> animalMap = new ArrayList<>();
    animals.stream().filter(a -> a.getBarn() != null).collect(Collectors.groupingBy(Animal::getBarn)).forEach((barn, a) -> animalMap.add(a));
    //barns are sorted beforehand to make sure animals are evenly distributed when added to the farm
    animalMap.sort(Comparator.comparingInt(List::size));

//    if no animals existed previously with the same favorite color, then a new barn is built
//    if all barns reach full capacity, a new barn is made and animals are evenly distributed
//    else animals are added to the barn with the least amount of animals
    if (animalMap.size() == 0) {
      Barn barn = new Barn(animal.getFavoriteColor().name() + animalMap.size(), animal.getFavoriteColor());
      barnRepository.save(barn);
      animal.setBarn(barn);
    } else if (animalMap.get(0).size() == 20) {
      Barn barn = new Barn(animal.getFavoriteColor().name() + animalMap.size(), animal.getFavoriteColor());
      barnRepository.save(barn);
      animal.setBarn(barn);
      distributeAnimals(animals.size(), animalMap, barn);
    } else {
      animal.setBarn(animalMap.stream().findFirst().orElse(null).stream().findFirst().orElse(null).getBarn());
    }
    animalRepository.save(animal);
    return animal;
  }

  @Override
  public void addToFarm(List<Animal> animals) {
    animals.forEach(this::addToFarm);
  }

//  called by the removeFromFarm method below. This method decides if a barn can be destroyed, or if animals must be reorganized
  private void organizeBarns(int animalSize, List<List<Animal>> animalMap) {
    int animalsPerBarn = animalSize / (animalMap.size() - 1);
    int remainder = animalSize % (animalMap.size() - 1);

    List<Animal> highestCapacityBarn = animalMap.get(animalMap.size() - 1);
    Barn reassignmentBarn = animalMap.stream().findFirst().orElse(null).stream().findFirst().orElse(null).getBarn();
    if (animalMap.get(0).size() + 1 < highestCapacityBarn.size()) {
      Animal reassignAnimal = highestCapacityBarn.get(highestCapacityBarn.size()-1).setBarn(reassignmentBarn);
      animalMap.get(0).add(reassignAnimal);
      highestCapacityBarn.remove(reassignAnimal);
      animalRepository.save(reassignAnimal);
    }

    if (animalsPerBarn <= 20 && remainder == 0) {
      List<Animal> animalsToBeMoved = animalMap.get(0);
      Barn toBeDestroyed = animalsToBeMoved.get(0).getBarn();
      animalsToBeMoved.forEach(animal -> animal.setBarn(null));
      animalRepository.saveAll(animalsToBeMoved);
      barnRepository.delete(toBeDestroyed);
      addToFarm(animalsToBeMoved);
    }

  }

  @Override
  public void removeFromFarm(Animal animal) {
    animalRepository.delete(animal);
    List<Animal> animals = animalRepository.findAll().stream().filter(a -> a.getFavoriteColor() == animal.getFavoriteColor()).collect(Collectors.toList());
    List<List<Animal>> animalMap = new ArrayList<>();
    animals.stream().collect(Collectors.groupingBy(Animal::getBarn)).forEach((barn, a) -> animalMap.add(a));
    animalMap.sort(Comparator.comparingInt(List::size));
    //checked to stop NPE in above method, along with not needed the call since all animals would just stay in a singular barn
    if (animalMap.size() > 1) {
      organizeBarns(animals.size(), animalMap);
    }
  }

  @Override
  public void removeFromFarm(List<Animal> animals) {
    animals.forEach(animal -> removeFromFarm(animalRepository.getOne(animal.getId())));
  }
}
